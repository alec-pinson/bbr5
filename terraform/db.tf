resource "azurerm_mssql_server" "this" {
  for_each                     = toset(var.environments)
  name                         = "${each.key}-bbr-sqlserver"
  resource_group_name          = azurerm_resource_group.this.name
  location                     = azurerm_resource_group.this.location
  version                      = "12.0"
  administrator_login          = var.database_admin_username
  administrator_login_password = var.database_admin_password
}

resource "azurerm_mssql_database" "bbr" {
  for_each       = toset(var.environments)
  name           = "bbr"
  server_id      = azurerm_mssql_server.this[each.key].id
  collation      = "Norwegian_100_CI_AS" # We run in Norwegian as the sorting order works correctly for accented characters
  license_type   = "LicenseIncluded"
  max_size_gb    = 1
  sku_name       = "Basic"
  zone_redundant = false
}

resource "azurerm_mssql_firewall_rule" "this" {
  for_each         = toset(azurerm_linux_web_app.bbr5.possible_outbound_ip_address_list)
  name             = "bbr-app-${each.key}"
  server_id        = azurerm_mssql_server.this["prod"].id
  start_ip_address = each.key
  end_ip_address   = each.key
}
