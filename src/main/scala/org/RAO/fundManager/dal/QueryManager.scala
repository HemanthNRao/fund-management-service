package org.RAO.fundManager.dal

import org.RAO.fundManager.ConfigManager

trait QueryManager
{
  val dbType = ConfigManager.get("db")
  val db = dbType match
  {
    case "sqlite3" => SqliteBackend
  }

}
