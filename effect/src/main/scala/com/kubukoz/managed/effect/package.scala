package com.kubukoz.managed

import cats.effect._
import cats.data.Kleisli
import com.kubukoz.Managed
import com.kubukoz.Managed._
import skolems.∀
import cats.implicits._

package object effect {
  def fromResource[F[_]: Bracket[*[_], Throwable], A](
      resource: Resource[F, A]
  ): Managed[F, A] =
    Managed.managed(∀[Use[F, A, *]](resource.use(_)))

}

object KleisliManagedExamples extends IOApp {

  def local[F[_], A](modValue: A => A): Managed[Kleisli[F, A, *], Unit] =
    Managed.managed {
      ∀[Managed.Use[Kleisli[F, A, *], Unit, *]] { use =>
        Kleisli { original =>
          use(()).run(modValue(original))
        }
      }
    }

  def set[A](s: A): Managed[Kleisli[IO, A, *], Unit] = local((_: A) => s)

  val printTraceId: Kleisli[IO, String, Unit] = Kleisli { traceId =>
    IO(println(traceId))
  }

  def run(args: List[String]): IO[ExitCode] = {
    val prog1 =
      //root
      printTraceId *>
        //nested1
        set("nested1").use(_ => printTraceId) *>
        //root
        printTraceId *>
        //nested2
        set("nested2").use(_ => printTraceId) *>
        //root
        printTraceId

    val prog2 = {
      //root
      Managed.liftF(printTraceId) *>
        set("nested1") *>
        //nested1
        Managed.liftF(printTraceId) *>
        set("nested2") *>
        //nested2
        Managed.liftF(printTraceId)
    }.use(_ => printTraceId) *> //nested2
      Managed.liftF(printTraceId).use(_ => printTraceId) //root, root

    IO(println("prog1")) *>
      prog1.run("root") *>
      IO(println("\n\nprog2")) *>
      prog2.run("root")
  }.as(ExitCode.Success)
}
