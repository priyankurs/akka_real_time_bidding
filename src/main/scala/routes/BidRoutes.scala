package routes

import java.util.UUID
import java.util.concurrent.TimeUnit


import actor.BiddingAgent
import akka.http.scaladsl.server.Directives.{concat, pathPrefix}
import com.example.JsonFormats
import com.sun.org.slf4j.internal.LoggerFactory
import com.typesafe.config.Config
import protocol.{BidRequest, BidResponse}

import scala.util.Failure
import scala.util.Success


import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout



class BidRoute(
                biddingAgent: ActorRef[BiddingAgent.Command],
                config: Config
              )(implicit system: ActorSystem[Nothing]) {



  private val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout: Timeout = Timeout(config.getDuration("akka.ask-execution-timeout").getSeconds, TimeUnit.SECONDS)

  val routes: Route = makeBidRoute

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._


  private def makeBidRoute: Route =
    pathPrefix("make-bid") {
      post {
        entity(as[BidRequest]) { req =>
          //log.info("Received request to make a bid [{}] from site [{}]", req.id, req.site.domain)

          onComplete(
            biddingAgent.ask(ref => BiddingAgent.MakeBid(req, ref))
          ) {
            case Failure(exception)                      =>
              log.warn("Making bid failed with exception [{}]", exception)
              complete(StatusCodes.InternalServerError)
            case Success(BiddingAgent.BidMade(campaign)) =>
              campaign match {
                case None                              =>
                  //log.info("No campaign found for bid [{}]", req.id)
                  complete(StatusCodes.NoContent)
                case Some((campaignId, banner, price)) =>
                  val response = BidResponse(
                    id           = UUID.randomUUID().toString.replace("-", ""),
                    bidRequestId = req.id,
                    price        = price,
                    adid         = Some(campaignId.toString),
                    banner       = Some(banner)
                  )
                  complete(StatusCodes.OK, response)
              }
          }
        }
       }
    }
}
