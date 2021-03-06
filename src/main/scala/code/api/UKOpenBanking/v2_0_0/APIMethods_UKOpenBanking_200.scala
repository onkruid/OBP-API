package code.api.UKOpenBanking.v2_0_0

import code.api.APIFailureNewStyle
import code.api.ResourceDocs1_4_0.SwaggerDefinitionsJSON
import code.api.util.APIUtil._
import code.api.util.ErrorMessages.{BankAccountNotFound, InvalidConnectorResponseForGetTransactionRequests210, UnknownError, UserNotLoggedIn, ViewNotFound}
import code.api.util.{ApiVersion, ErrorMessages}
import code.bankconnectors.Connector
import code.model._
import code.model.AccountId
import code.util.Helper
import code.views.Views
import net.liftweb.http.rest.RestHelper
import code.api.util.ErrorMessages._
import scala.collection.immutable.Nil
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait APIMethods_UKOpenBanking_200 {
  //needs to be a RestHelper to get access to JsonGet, JsonPost, etc.
  self: RestHelper =>

  val ImplementationsUKOpenBanking200 = new Object() {
    val implementedInApiVersion: ApiVersion = ApiVersion.ukOpenBankingV200 // was noV

    val resourceDocs = ArrayBuffer[ResourceDoc]()
    val apiRelations = ArrayBuffer[ApiRelation]()
    val codeContext = CodeContext(resourceDocs, apiRelations)

    resourceDocs += ResourceDoc(
      getAccountList,
      implementedInApiVersion,
      "getAccountList",
      "GET",
      "/accounts",
      "UK Open Banking: Get Account List",
      s"""
         |Reads a list of bank accounts, with balances where required.
         |It is assumed that a consent of the PSU to this access is already given and stored on the ASPSP system.
         |
        |${authenticationRequiredMessage(true)}
        |
        |This call is work in progress - Experimental!
         |""",
      emptyObjectJson,
      SwaggerDefinitionsJSON.accountsJsonUKOpenBanking_v200,
      List(ErrorMessages.UserNotLoggedIn,ErrorMessages.UnknownError),
      Catalogs(notCore, notPSD2, notOBWG),
      List(apiTagUKOpenBanking, apiTagAccount, apiTagPrivateData))

    apiRelations += ApiRelation(getAccountList, getAccountList, "self")

    lazy val getAccountList : OBPEndpoint = {
      //get private accounts for all banks
      case "accounts" :: Nil JsonGet _ => {
        cc =>
          for {
            (user, callContext) <- extractCallContext(ErrorMessages.UserNotLoggedIn, cc)
            u <- unboxFullAndWrapIntoFuture{ user }
            availablePrivateAccounts <- Views.views.vend.getPrivateBankAccountsFuture(u)
            accounts <- {Connector.connector.vend.getBankAccountsFuture(availablePrivateAccounts, callContext)}
          } yield {
            (JSONFactory_UKOpenBanking_200.createAccountsListJSON(accounts.getOrElse(Nil)), callContext)
          }
      }
    }
  
    resourceDocs += ResourceDoc(
      getAccountTransactions,
      implementedInApiVersion,
      "getAccountTransactions",
      "GET",
      "/accounts/ACCOUNT_ID/transactions",
      "UK Open Banking: Get Account Transactions",
      s"""
         |Reads account data from a given account addressed by “account-id”. 
         |${authenticationRequiredMessage(true)}
         |
         |This call is work in progress - Experimental!
         |""",
      emptyObjectJson,
      SwaggerDefinitionsJSON.transactionsJsonUKV200,
      List(UserNotLoggedIn,UnknownError),
      Catalogs(Core, PSD2, OBWG),
      List(apiTagUKOpenBanking, apiTagTransaction, apiTagPrivateData))
  
    lazy val getAccountTransactions : OBPEndpoint = {
      //get private accounts for all banks
      case "accounts" :: AccountId(accountId) :: "transactions" :: Nil JsonGet _ => {
        cc =>
          for {
            (user, callContext) <- extractCallContext(UserNotLoggedIn, cc)
            u <- unboxFullAndWrapIntoFuture{ user }
            bankAccount <- Future { BankAccount(BankId(defaultBankId), accountId, callContext) } map {
              x => fullBoxOrException(x ~> APIFailureNewStyle(BankAccountNotFound, 400, Some(cc.toLight)))
            } map { unboxFull(_) }
            view <- Views.views.vend.viewFuture(ViewId("owner"), BankIdAccountId(bankAccount.bankId, bankAccount.accountId)) map {
              x => fullBoxOrException(x ~> APIFailureNewStyle(ViewNotFound, 400, Some(cc.toLight)))
            } map { unboxFull(_) }
            params <- Future { getTransactionParams(callContext.get.requestHeaders)} map {
              x => fullBoxOrException(x ~> APIFailureNewStyle(UnknownError, 400, Some(cc.toLight)))
            } map { unboxFull(_) }
          
            transactionRequests <- Future { Connector.connector.vend.getTransactionRequests210(u, bankAccount)} map {
              x => fullBoxOrException(x ~> APIFailureNewStyle(InvalidConnectorResponseForGetTransactionRequests210, 400, Some(cc.toLight)))
            } map { unboxFull(_) }
          
            transactions <- Future { bankAccount.getModeratedTransactions(user, view, callContext, params: _*)} map {
              x => fullBoxOrException(x ~> APIFailureNewStyle(UnknownError, 400, Some(cc.toLight)))
            } map { unboxFull(_) }
          
          } yield {
            (JSONFactory_UKOpenBanking_200.createTransactionsJson(transactions, transactionRequests), callContext)
          }
      }
    }


    resourceDocs += ResourceDoc(
      getAccount,
      implementedInApiVersion,
      "getAccount",
      "GET",
      "/accounts/ACCOUNT_ID",
      "UK Open Banking: Get Account",
      s"""
         |Reads a bank account, with balances where required.
         |It is assumed that a consent of the PSU to this access is already given and stored on the ASPSP system.
         |
        |${authenticationRequiredMessage(true)}
        |
        |This call is work in progress - Experimental!
         |""",
      emptyObjectJson,
      SwaggerDefinitionsJSON.accountsJsonUKOpenBanking_v200,
      List(ErrorMessages.UserNotLoggedIn,ErrorMessages.UnknownError),
      Catalogs(notCore, notPSD2, notOBWG),
      List(apiTagUKOpenBanking, apiTagAccount, apiTagPrivateData))

    apiRelations += ApiRelation(getAccount, getAccount, "self")

    lazy val getAccount : OBPEndpoint = {
      //get private accounts for all banks
      case "accounts" :: AccountId(accountId) :: Nil JsonGet _ => {
        cc =>
          for {
            (user, callContext) <- extractCallContext(ErrorMessages.UserNotLoggedIn, cc)
            u <- unboxFullAndWrapIntoFuture{ user }
            availablePrivateAccounts <- Views.views.vend.getPrivateBankAccountsFuture(u) map {
              _.filter(_.accountId.value == accountId.value)
            }
            accounts <- {Connector.connector.vend.getBankAccountsFuture(availablePrivateAccounts, callContext)}
          } yield {
            (JSONFactory_UKOpenBanking_200.createAccountJSON(accounts.getOrElse(Nil)), callContext)
          }
      }
    }
  
    resourceDocs += ResourceDoc(
      getAccountBalances,
      implementedInApiVersion,
      "getAccountBalances",
      "GET",
      "/accounts/ACCOUNT_ID/balances",
      "UK Open Banking: Get Account Balances",
      s"""
         |An AISP may retrieve the account balance information resource for a specific AccountId 
         |(which is retrieved in the call to GET /accounts).
         |
         |${authenticationRequiredMessage(true)}
         |
         |This call is work in progress - Experimental!
         |""",
      emptyObjectJson,
      SwaggerDefinitionsJSON.accountBalancesUKV200,
      List(ErrorMessages.UserNotLoggedIn,ErrorMessages.UnknownError),
      Catalogs(notCore, notPSD2, notOBWG),
      List(apiTagUKOpenBanking, apiTagAccount, apiTagPrivateData))
  
    lazy val getAccountBalances : OBPEndpoint = {
      //get private accounts for all banks
      case "accounts" :: AccountId(accountId) :: "balances" :: Nil JsonGet _ => {
        cc =>
          for {
            (user, callContext) <- extractCallContext(UserNotLoggedIn, cc)
            u <- unboxFullAndWrapIntoFuture{ user }
        
            account <- Future { BankAccount(BankId(defaultBankId), accountId, callContext) } map {
              x => fullBoxOrException(x ~> APIFailureNewStyle(BankAccountNotFound, 400, Some(cc.toLight)))
            } map { unboxFull(_) }
        
            view <- Views.views.vend.viewFuture(ViewId("owner"), BankIdAccountId(account.bankId, account.accountId)) map {
              x => fullBoxOrException(x ~> APIFailureNewStyle(ViewNotFound, 400, Some(cc.toLight)))
            } map { unboxFull(_) }
        
            _ <- Helper.booleanToFuture(failMsg = s"${UserNoPermissionAccessView} Current VIEW_ID (${view.viewId.value})") {(u.hasViewAccess(view))}
        
            moderatedAccount <- Future {account.moderatedBankAccount(view, user)} map {
              x => fullBoxOrException(x ~> APIFailureNewStyle(UnknownError, 400, Some(cc.toLight)))
            } map { unboxFull(_) }
        
          } yield {
            (JSONFactory_UKOpenBanking_200.createAccountBalanceJSON(moderatedAccount), callContext)
          }
      }
    }
  
    resourceDocs += ResourceDoc(
      getBalances,
      implementedInApiVersion,
      "getBalances",
      "GET",
      "/balances",
      "UK Open Banking: Get Balances",
      s"""
         |
         |If an ASPSP has implemented the bulk retrieval endpoints - 
         |an AISP may optionally retrieve the account information resources in bulk.
         |This will retrieve the resources for all authorised accounts linked to the account-request.
         |
         |${authenticationRequiredMessage(true)}
         |
         |This call is work in progress - Experimental!
         |""",
      emptyObjectJson,
      SwaggerDefinitionsJSON.accountBalancesUKV200,
      List(ErrorMessages.UserNotLoggedIn,ErrorMessages.UnknownError),
      Catalogs(notCore, notPSD2, notOBWG),
      List(apiTagUKOpenBanking, apiTagAccount, apiTagPrivateData))
  
    lazy val getBalances : OBPEndpoint = {
      //get private accounts for all banks
      case "balances" :: Nil JsonGet _ => {
        cc =>
          for {
            (user, callContext) <- extractCallContext(UserNotLoggedIn, cc)
            u <- unboxFullAndWrapIntoFuture{ user }

            availablePrivateAccounts <- Views.views.vend.getPrivateBankAccountsFuture(u)
          
            accounts <- {Connector.connector.vend.getBankAccountsFuture(availablePrivateAccounts, callContext)}
          
          } yield {
            (JSONFactory_UKOpenBanking_200.createBalancesJSON(accounts.getOrElse(Nil)), callContext)
          }
      }
    }

  }

}



