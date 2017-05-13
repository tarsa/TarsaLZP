/*
 * Copyright (C) 2016 - 2017 Piotr Tarsa ( http://github.com/tarsa )
 *
 *  This software is provided 'as-is', without any express or implied
 *  warranty.  In no event will the author be held liable for any damages
 *  arising from the use of this software.
 *
 *  Permission is granted to anyone to use this software for any purpose,
 *  including commercial applications, and to alter it and redistribute it
 *  freely, subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 *  3. This notice may not be removed or altered from any source distribution.
 *
 */
package _infrastructure.specs

import _infrastructure.global.AkkaForTesting
import akka.actor.ActorSystem
import org.scalatest._

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

sealed trait SpecBase extends MustMatchers with Inside {
  this: SpecBase.HasBehavior =>

  def typeBehavior[T](implicit classTag: ClassTag[T]): Unit = {
    import scala.language.reflectiveCalls
    behavior of classTag.runtimeClass.getSimpleName
  }
}

object SpecBase {
  type HasBehavior = {
    def behavior: {
      def of(description: scala.Predef.String)(
          implicit pos: org.scalactic.source.Position): Unit
    }
  }
}

abstract class SyncSpecBase extends FlatSpec with SpecBase

abstract class FixtureSyncSpecBase extends fixture.FlatSpec with SpecBase

sealed trait AsyncSpecMixin { this: AsyncTestSuite =>
  override implicit def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}

abstract class AsyncSpecBase
    extends AsyncFlatSpec
    with SpecBase
    with AsyncSpecMixin

abstract class FixtureAsyncSpecBase
    extends fixture.AsyncFlatSpec
    with SpecBase
    with AsyncSpecMixin

sealed trait AkkaSpecMixin {
  protected implicit val actorSystem: ActorSystem =
    AkkaForTesting.actorSystem
}

abstract class AkkaSpecBase extends AsyncSpecBase with AkkaSpecMixin

abstract class FixtureAkkaSpecBase
    extends FixtureAsyncSpecBase
    with AkkaSpecMixin
