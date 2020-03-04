package com.kubukoz

import cats._
import cats.implicits._
import cats.effect._
import skolems._
import cats.data.Kleisli

/**
  * A continuation-based alternative to Resource.
  */
trait Managed[F[_], A] {
  def use[B](f: A => F[B]): F[B]
}

object Managed {
  type Use2[F[_], A, b] = (A => F[b]) => F[b]
  type Use[F[_], A] = ∀[Use2[F, A, *]]

  def managed[F[_], A](use: Use[F, A]): Managed[F, A] = Instance(use)

  def liftF[F[_]: FlatMap, A](fa: F[A]): Managed[F, A] = managed(∀[Use2[F, A, *]](fa.flatMap(_)))

  def fromResource[F[_]: Bracket[*[_], Throwable], A](resource: Resource[F, A]): Managed[F, A] =
    managed(∀[Use2[F, A, *]](resource.use(_)))

  implicit def managedMonad[F[_]: Monad]: Monad[Managed[F, *]] =
    new StackSafeMonad[Managed[F, *]] {

      def flatMap[A, B](fa: Managed[F, A])(f: A => Managed[F, B]): Managed[F, B] =
        managed {
          ∀[Use2[F, B, *]](use => fa.use(f(_).use(use)))
        }

      def pure[A](x: A): Managed[F, A] = Managed.managed(∀[Use2[F, A, *]](_(x)))
    }

  final case class Instance[F[_], A](useManaged: Use[F, A]) extends Managed[F, A] {
    def use[B](f: A => F[B]): F[B] = useManaged[B](f)
  }
}

object KleisliManagedExamples extends IOApp {

  def local[F[_], A](modValue: A => A): Managed[Kleisli[F, A, *], Unit] =
    Managed.managed {
      ∀[Managed.Use2[Kleisli[F, A, *], Unit, *]] { use =>
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
