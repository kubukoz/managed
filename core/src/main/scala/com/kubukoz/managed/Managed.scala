package com.kubukoz.managed

import cats._
import cats.implicits._
import skolems._

/**  A continuation-based alternative to Resource.
  */
final case class Managed[F[_], A](private val useManaged: Managed.Use0[F, A]) {
  def use[B](f: A => F[B]): F[B] = useManaged[B](f)
}

object Managed {
  // Usage of a resource. For values, use like ∀[Use[F, A, *]](cb => cb(resource)).
  // For types, better go with Use0[F, A]
  type Use[F[_], A, b] = (A => F[b]) => F[b]
  type Use0[F[_], A] = ∀[Use[F, A, *]]

  def managed[F[_], A](use: Use0[F, A]): Managed[F, A] = new Managed(use)

  def liftF[F[_]: FlatMap, A](fa: F[A]): Managed[F, A] = managed(
    ∀[Use[F, A, *]](fa.flatMap(_))
  )

  implicit def managedMonad[F[_]]: Monad[Managed[F, *]] =
    new StackSafeMonad[Managed[F, *]] {

      def flatMap[A, B](
          fa: Managed[F, A]
      )(f: A => Managed[F, B]): Managed[F, B] =
        managed {
          ∀[Use[F, B, *]](use => fa.use(f(_).use(use)))
        }

      def pure[A](x: A): Managed[F, A] = Managed.managed(∀[Use[F, A, *]](_(x)))
    }

  implicit def eqManaged[A, F[_]: Applicative](implicit
      eqEffect: Eq[F[A]]
  ): Eq[Managed[F, A]] =
    Eq.by(_.use(_.pure[F]))
}
