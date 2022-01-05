import actor.BiddingAgent
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.javadsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.sun.org.slf4j.internal.LoggerFactory
import com.typesafe.config.ConfigFactory
import config.HttpConfig
import data.CampaignData
import protocol.Campaign
import routes.BidRoute
import scala.util.Failure
import scala.util.Success

import scala.concurrent.ExecutionContext

class Service {

  private val log = LoggerFactory.getLogger(this.getClass)

  lazy val campaigns: Seq[Campaign] = CampaignData.activeCampaigns

  val rootBehavior: Behavior[Nothing] =
    Behaviors.setup[Nothing] { context =>
      val biddingAgentActor = context.spawn(BiddingAgent(campaigns), "BiddingAgentActor")
      context.watch(biddingAgentActor)

      startHttpServer(new BidRoute(biddingAgentActor, config).routes)
      Behaviors.empty
    }

  implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](rootBehavior, "ad-realtime-bidder")
  implicit val ec: ExecutionContext         = system.executionContext

  private val config     = ConfigFactory.load()
  private val httpConfig = HttpConfig.fromConfig(config)

  private def startHttpServer(routes: Route): Unit = {
    val bindFuture = Http().newServerAt(httpConfig.host, httpConfig.port).bind(routes)
    bindFuture.onComplete {
      case Success(_)         =>
//        log.info("Ad Service running on [{}]:[{}]", httpConfig.host, httpConfig.port)
      case Failure(exception) =>
        log.warn("Service shutting down because [{}]", exception)
        system.terminate()
    }
  }

}