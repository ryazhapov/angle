//package io.ryazhapov
//
//import io.ryazhapov.config.{ConfigService, DatabaseConfig}
//import io.ryazhapov.database.repositories.auth.UserRepository
//import io.ryazhapov.database.services.TransactorService
//import org.testcontainers.containers.PostgreSQLContainer
//import zio.blocking.{Blocking, effectBlocking}
//import zio.test.{DefaultRunnableSpec, assertCompletes, assertTrue, suite, testM}
//import zio.{Managed, ZIO, ZLayer}
//import zio.blocking.{Blocking, effectBlocking}
//import zio.clock.Clock
//import zio.{Managed, ZIO, ZLayer}
//
//import java.time.ZonedDateTime
//
//object UserRepositorySpec extends DefaultRunnableSpec {
//
//  override def spec = {
//
//    val containerLayer = Managed.make {
//      effectBlocking {
//        val container = new PostgreSQLContainer("postgres:14.1")
//        container.withUsername("postgres")
//        container.start()
//        container
//      }
//    }(r => effectBlocking(r.close()).orDie).toLayer
//
//    val configuration =
//      ZLayer.fromService[PostgreSQLContainer[Nothing], DatabaseConfig](
//        container =>
//          DatabaseConfig(
//            container.getDriverClassName,
//            container.getJdbcUrl,
//            container.getUsername,
//            container.getPassword
//          )
//      )
//
//    val datasource = configuration >+> Blocking.live >+> Clock.live >+> TransactorService.live
//    val repository = datasource >>> UserRepository.live
//    val env = Blocking.live >+> containerLayer >+> repository
//
//    def runMigrations(jdbcUrl: String, username: String, password: String) = {
//      effectBlocking {
//        Flyway
//          .configure()
//          .dataSource(jdbcUrl, username, password)
//          .schemas("public")
//          .load()
//          .migrate()
//      }
//    }
//
//    suite("postgres repository")(
//      testM("can save in repository") {
//        val eventId = "event id"
//
//        val eventToSave = Event(
//          eventId,
//          "title",
//          EventType.online,
//          ZonedDateTime.now(),
//          "somewhere",
//          Some(100),
//          Seq(
//            EventTalk("some title", "speaker", "description")
//          )
//        )
//        val f = for {
//          container <- ZIO.service[PostgreSQLContainer[Nothing]]
//          _ <- runMigrations(
//            container.getJdbcUrl,
//            container.getUsername,
//            container.getPassword
//          )
//          _ <- ZIO.accessM[EventRepository](_.get.save(eventToSave)).catchSome {
//              case e: DatabaseError => {
//                println(e)
//                ZIO.unit
//              }
//            }
//          all <- ZIO.accessM[EventRepository](_.get.all())
//        } yield assertTrue(all.isEmpty)
//
//        f.provideLayer(env)
//      }
//    )
//  }
//}
