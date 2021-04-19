package org.RAO.fundManager.dal

import org.RAO.fundManager.ConfigManager

import java.sql.DriverManager

object SqliteBackend extends DBBackend
{
  Class.forName(ConfigManager.get("sqlite3.driver"))
  var url=ConfigManager.get("sqlite3.url")
  var user=ConfigManager.get("sqlite3.user")
  var pass=ConfigManager.get("sqlite3.pass")
  private def dbConn=DriverManager.getConnection(url,user,pass)
  override def getConnection = dbConn

  //Execute the block for setting up the tables
  {
    val createFunds = """CREATE TABLE if not exists funds(id varchar(50) PRIMARY KEY,name varchar(70) not null,amount double,date DATE not null,description text,balance double);"""
    val dropFunds = """drop table if exists funds"""
    val createTransactions = """CREATE TABLE if not exists transactions(id varchar(50) PRIMARY KEY, amount double not null, who varchar(30), date date, time time, description varchar(300), type varchar(20), fundId varchar(50),balance double,foreign key(fundId) references funds(id) ON DELETE CASCADE ON UPDATE CASCADE);
                               |""".stripMargin
    val dropTransactions = """drop table if exists transactions"""
    queyWithNoResult(createFunds)
    queyWithNoResult(createTransactions)
  }
}
