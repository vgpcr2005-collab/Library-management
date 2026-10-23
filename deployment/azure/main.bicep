@description('Azure region for the library website and data services.')
param location string = resourceGroup().location

@description('Globally unique, 2-60 character App Service name.')
@minLength(2)
@maxLength(60)
param webAppName string

@description('Globally unique name for the Azure Container Registry.')
@minLength(5)
@maxLength(50)
param registryName string

@description('Globally unique, lowercase MySQL Flexible Server name.')
@minLength(3)
@maxLength(63)
param mysqlServerName string

@description('MySQL administrator login. Do not reuse the application account.')
@minLength(3)
@maxLength(32)
param mysqlAdminLogin string

@secure()
@description('Strong administrator password for initial MySQL setup; enter through the Azure deployment UI.')
param mysqlAdminPassword string

@description('Your current public IPv4 address. It is allowed to connect to MySQL for setup; replace it if it changes.')
param setupClientIp string

resource mysql 'Microsoft.DBforMySQL/flexibleServers@2023-12-30' = {
  name: mysqlServerName
  location: location
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  properties: {
    version: '8.0.21'
    administratorLogin: mysqlAdminLogin
    administratorLoginPassword: mysqlAdminPassword
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
    storage: {
      storageSizeGB: 32
    }
    network: {
      publicNetworkAccess: 'Enabled'
    }
  }
}

resource libraryDb 'Microsoft.DBforMySQL/flexibleServers/databases@2023-12-30' = {
  parent: mysql
  name: 'alliance_library'
  properties: {
    charset: 'utf8mb4'
    collation: 'utf8mb4_0900_ai_ci'
  }
}

resource registry 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: registryName
  location: location
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: false
    publicNetworkAccess: 'Enabled'
  }
}

resource appServicePlan 'Microsoft.Web/serverfarms@2022-03-01' = {
  name: '${webAppName}-plan'
  location: location
  kind: 'linux'
  sku: {
    name: 'B1'
    tier: 'Basic'
    capacity: 1
  }
  properties: {
    reserved: true
  }
}

resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: take('${webAppName}-kv', 24)
  location: location
  properties: {
    tenantId: tenant().tenantId
    sku: {
      family: 'A'
      name: 'standard'
    }
    enableRbacAuthorization: true
    enableSoftDelete: true
    softDeleteRetentionInDays: 90
    publicNetworkAccess: 'Enabled'
  }
}

resource webApp 'Microsoft.Web/sites@2022-03-01' = {
  name: webAppName
  location: location
  kind: 'app,linux,container'
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    serverFarmId: appServicePlan.id
    httpsOnly: true
    keyVaultReferenceIdentity: 'SystemAssigned'
    siteConfig: {
      alwaysOn: true
      linuxFxVersion: 'DOCKER|${registry.properties.loginServer}/smart-library:latest'
      acrUseManagedIdentityCreds: true
      ftpsState: 'Disabled'
      minTlsVersion: '1.2'
      http20Enabled: true
      appSettings: [
        {
          name: 'APP_ENV'
          value: 'production'
        }
        {
          name: 'WEBSITES_PORT'
          value: '8080'
        }
        {
          name: 'LIBRARY_DB_URL'
          value: 'jdbc:mysql://${mysql.properties.fullyQualifiedDomainName}:3306/alliance_library?sslMode=VERIFY_IDENTITY&serverTimezone=UTC'
        }
      ]
    }
  }
  dependsOn: [
    appServicePlan
    registry
    mysql
  ]
}

resource setupIpFirewall 'Microsoft.DBforMySQL/flexibleServers/firewallRules@2023-12-30' = {
  parent: mysql
  name: 'setup-client'
  properties: {
    startIpAddress: setupClientIp
    endIpAddress: setupClientIp
  }
}

var appOutboundIps = split(webApp.properties.possibleOutboundIpAddresses, ',')
resource appServiceFirewall 'Microsoft.DBforMySQL/flexibleServers/firewallRules@2023-12-30' = [for (ip, index) in appOutboundIps: {
  parent: mysql
  name: 'app-service-${index}'
  properties: {
    startIpAddress: ip
    endIpAddress: ip
  }
}]

resource appAcrPull 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(registry.id, webApp.id, 'AcrPull')
  scope: registry
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '7f951dda-4ed3-4680-a7ca-43fe172d538d')
    principalId: webApp.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

resource appKeyVaultSecrets 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(keyVault.id, webApp.id, 'KeyVaultSecretsUser')
  scope: keyVault
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '4633458b-17de-408a-b874-0445c86b69e6')
    principalId: webApp.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

output webAppUrl string = 'https://${webApp.properties.defaultHostName}'
output mysqlHost string = mysql.properties.fullyQualifiedDomainName
output registryLoginServer string = registry.properties.loginServer
output keyVaultName string = keyVault.name
output resourceGroupName string = resourceGroup().name
output appServiceOutboundIpAddresses string = webApp.properties.possibleOutboundIpAddresses
