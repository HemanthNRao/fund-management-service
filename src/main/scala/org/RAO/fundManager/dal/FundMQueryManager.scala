package org.RAO.fundManager.dal

import java.util.Locale.Category
import scala.collection.mutable

object FundMQueryManager extends QueryManager
{
  def insertFund(id:String, name:String, amount:Double, date:String,description:String): Unit =
  {
    db.queyWithNoResult("insert into funds(id, name, amount, date, description, balance) values(?,?,?,?,?,?)",Array(id, name, amount, date, description, amount))
  }
  def getAllFunds= {
    db.queryWithResult("SELECT * FROM funds",Array())
  }

  def getFund(id:String)=
  {
    db.queryWithResult("SELECT * FROM funds WHERE id=?", Array(id))
  }

  def deleteFund(id:String)=
  {
  db.queyWithNoResult("DELETE FROM funds WHERE id=?",Array(id))
  }

  def insertTransaction(id:String, amount:Double, who:String, category: String, fundId:String, description:String, balance:Double, date:String, time:String): Unit =
  {
    db.queyWithNoResult("INSERT INTO transactions(id, amount, who, date, time, description, type, fundId, balance) VALUES(?,?,?,?,?,?,?,?,?)", Array(id, amount, who, date, time, description, category, fundId, balance))
  }

  def getFundBalance(id:String)=
  {
    db.queryWithSingleResult[Double]("SELECT balance FROM funds WHERE id=?", Array(id))
  }

  def updateFundBalance(id:String, balance:Double)=
  {
    db.queyWithNoResult("UPDATE funds SET  balance=? where id=?", Array(balance, id))
  }

  def getAllTransactions= {
    db.queryWithResult("SELECT * FROM transactions",Array())
  }

  def getTransactions(id:String)=
  {
    db.queryWithResult("SELECT * FROM transactions WHERE fundId=?", Array(id))
  }

  def deleteTransaction(id:String)=
  {
    db.queyWithNoResult("DELETE FROM transactions WHERE id=?",Array(id))
  }

  def getTransactionWithType(id:String, cat:String)=
  {
    db.queryWithResult(" SELECT * FROM transactions WHERE fundId=? and type=?", Array(id,cat))
  }
}
