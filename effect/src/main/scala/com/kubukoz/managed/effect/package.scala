package com.kubukoz.managed

import cats.effect._
import com.kubukoz.managed.Managed
import com.kubukoz.managed.Managed._
import skolems.∀

package object effect {
  def fromResource[F[_]: Bracket[*[_], Throwable], A](
      resource: Resource[F, A]
  ): Managed[F, A] =
    Managed.managed(∀[Use[F, A, *]](resource.use(_)))

}
