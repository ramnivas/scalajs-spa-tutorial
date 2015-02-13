package spatutorial.server

import akka.actor.ActorSystem
import spatutorial.shared.Api
import spray.routing.SimpleRoutingApp

import scala.util.Properties

object Router extends autowire.Server[String, upickle.Reader, upickle.Writer]{
  def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)
  def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}

object MainApp extends SimpleRoutingApp {
  def main(args: Array[String]): Unit = {
    // create an Actor System
    implicit val system = ActorSystem("SPA")
    // use system's dispatcher as ExecutionContext for futures etc.
    implicit val context = system.dispatcher

    val port = Properties.envOrElse("SPA_PORT", "8080").toInt

    val apiService = new ApiService

    startServer("0.0.0.0", port = port) {
      get {
        // Try serving from "web", if not serve the index.html page to work with pretty urls
        {
          getFromResourceDirectory("web")
        } ~ {
          getFromResource("web/index.html")
        }
      } ~ post {
        path("api" / Segments) { s =>
          extract(_.request.entity.asString) { e =>
            complete {
              // handle API requests via autowire
              Router.route[Api](apiService)(
                autowire.Core.Request(s, upickle.read[Map[String, String]](e))
              )
            }
          }
        }
      }
    }
  }
}
