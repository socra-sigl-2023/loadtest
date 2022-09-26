package socrate


import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

case class EnvironmentVariableException(private val message: String = "",
                                        private val cause: Throwable = None.orNull) extends Exception(message, cause)

class SocrateSimulation extends Simulation {

  @throws(classOf[EnvironmentVariableException])
  def getEnvOrThrow(name: String): String = {
    val env = System.getenv(name)
    if (env != null) env else throw EnvironmentVariableException(s"missing env var ${name}")
  }
  private val backendUrl: String = getEnvOrThrow("LOADTEST_BACKEND_URL")
  private val frontendUrl: String = getEnvOrThrow("LOADTEST_FRONTEND_URL")
  private val constantConcurrentUsersNumber: String = getEnvOrThrow("LOADTEST_CONSTANT_CONCURRENT_USERS")
  private val loadtestDuration: String = getEnvOrThrow("LOADTEST_DURATION")
  private val numberOfProductCategories = 18

  private val httpProtocol = http
    .baseUrl(backendUrl)
    .inferHtmlResources()
    .acceptHeader("*/*")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (X11; Linux x86_64; rv:104.0) Gecko/20100101 Firefox/104.0")

  def nextRandomProductCategory: Int = {
    val rand = new Random()
    // adds +1 since nextInt is 0-indexed and product categories starts
    // at 1.
    rand.nextInt(numberOfProductCategories) + 1
  }

  def nextRandDouble(base: Double): Double = {
    val rand = new Random()
    val pair = rand.nextInt(2)
    val bias = rand.nextDouble() + 2
    if (pair % 2 == 0) base + bias else base - bias
  }

  def getRandomLat: Double = {
    val base: Double = 52.5303808
    nextRandDouble(base)
  }

  def getRandomLong: Double = {
    val base: Double = 13.4283264
    nextRandDouble(base)
  }

  private val headers_0 = Map(
    "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
    "Cache-Control" -> "no-cache",
    "Pragma" -> "no-cache",
    "Upgrade-Insecure-Requests" -> "1"
  )

  private val headers_1 = Map(
    "Accept" -> "image/avif,image/webp,*/*",
    "Cache-Control" -> "no-cache",
    "Pragma" -> "no-cache"
  )

  private val options_header = Map(
    "Access-Control-Request-Headers" -> "content-type",
    "Access-Control-Request-Method" -> "POST",
    "Cache-Control" -> "no-cache",
    "Origin" -> frontendUrl,
    "Pragma" -> "no-cache"
  )

  private val get_header = Map(
    "Cache-Control" -> "no-cache",
    "Origin" -> frontendUrl,
    "Pragma" -> "no-cache"
  )

  private val post_header = Map(
    "Cache-Control" -> "no-cache",
    "Content-Type" -> "application/json",
    "Origin" -> frontendUrl,
    "Pragma" -> "no-cache"
  )


  private val uri2 = "socrate.loc"

  val feeder: Iterator[Map[String, Double]] = Iterator.continually {
    Map(
      "lat" -> getRandomLat,
      "long" -> getRandomLong,
      "category1" -> nextRandomProductCategory,
      "category2" -> nextRandomProductCategory)
  }

  private val scn = scenario("SocrateSimulation")
    .feed(feeder)
    .exec(
      http("get-frontend")
        .get(s"$frontendUrl/")
        .headers(headers_0)
        .resources(
          http("options-discounts-1")
            .options("/v1/discounts")
            .headers(options_header),
          http("post-discounts-1")
            .post("/v1/discounts")
            .headers(post_header)
            .body(StringBody(
              """
                 |{"user":{"latitude":${lat},"longitude":${long}}}
                 |""".stripMargin)),
          http("get-categories-1")
            .get("/v1/categories")
            .headers(get_header)
        )
    )
    .pause(3)
    .exec(
      http("options-category-1")
        .options("/v1/products/${category1}")
        .headers(options_header)
        .resources(
          http("post-category-1")
            .post("/v1/products/${category1}")
            .headers(post_header)
            .body(StringBody(
              """
                |{"user":{"latitude":${lat},"longitude":${long}}}
                |""".stripMargin))
        ))
        .pause(1)
        .exec(
          http("get-categories-2")
            .get("/v1/categories")
            .headers(get_header)
            .resources(
              http("options-discounts-2")
                .options("/v1/discounts")
                .headers(options_header),
              http("post-discounts-2")
                .post("/v1/discounts")
                .headers(post_header)
                .body(StringBody(
                  """
                    |{"user":{"latitude":${lat},"longitude":${long}}}
                    |""".stripMargin))
            )
        )
        .pause(2)
        .exec(
          http("options-category-2")
            .options("/v1/products/${category2}")
            .headers(options_header)
            .resources(
              http("post-category-2")
                .post("/v1/products/${category2}")
                .headers(post_header)
                .body(StringBody(
                  """
                    |{"user":{"latitude":${lat},"longitude":${long}}}
                    |""".stripMargin))
            )
        )

        setUp(scn.inject(constantConcurrentUsers(constantConcurrentUsersNumber.toInt) during (loadtestDuration.toInt minutes)))
          .protocols(httpProtocol)
}
