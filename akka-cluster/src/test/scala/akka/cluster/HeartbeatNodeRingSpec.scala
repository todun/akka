/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.cluster

import org.scalatest.WordSpec
import org.scalatest.Matchers
import akka.actor.Address
import akka.routing.ConsistentHash
import scala.concurrent.duration._
import scala.collection.immutable

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class HeartbeatNodeRingSpec extends WordSpec with Matchers {

  val aa = UniqueAddress(Address("akka.tcp", "sys", "aa", 2552), 1)
  val bb = UniqueAddress(Address("akka.tcp", "sys", "bb", 2552), 2)
  val cc = UniqueAddress(Address("akka.tcp", "sys", "cc", 2552), 3)
  val dd = UniqueAddress(Address("akka.tcp", "sys", "dd", 2552), 4)
  val ee = UniqueAddress(Address("akka.tcp", "sys", "ee", 2552), 5)
  val ff = UniqueAddress(Address("akka.tcp", "sys", "ff", 2552), 6)

  val nodes = Set(aa, bb, cc, dd, ee, ff)

  "A HashedNodeRing" must {

    "pick specified number of nodes as receivers" in {
      val ring = HeartbeatNodeRing(cc, nodes, Set.empty, 3)
      ring.myReceivers should be(ring.receivers(cc))

      nodes foreach { n ⇒
        val receivers = ring.receivers(n)
        receivers.size should be(3)
        receivers should not contain (n)
      }
    }

    "pick specified number of nodes + unreachable as receivers" in {
      val ring = HeartbeatNodeRing(cc, nodes, unreachable = Set(aa, dd, ee), monitoredByNrOfMembers = 3)
      ring.myReceivers should be(ring.receivers(cc))

      ring.receivers(aa) should be(Set(bb, cc, dd, ff)) // unreachable ee skipped
      ring.receivers(bb) should be(Set(cc, dd, ee, ff)) // unreachable aa skipped
      ring.receivers(cc) should be(Set(dd, ee, ff, bb)) // unreachable aa skipped
      ring.receivers(dd) should be(Set(ee, ff, aa, bb, cc))
      ring.receivers(ee) should be(Set(ff, aa, bb, cc))
      ring.receivers(ff) should be(Set(aa, bb, cc)) // unreachable dd and ee skipped
    }

    "pick all except own as receivers when less than total number of nodes" in {
      val expected = Set(aa, bb, dd, ee, ff)
      HeartbeatNodeRing(cc, nodes, Set.empty, 5).myReceivers should be(expected)
      HeartbeatNodeRing(cc, nodes, Set.empty, 6).myReceivers should be(expected)
      HeartbeatNodeRing(cc, nodes, Set.empty, 7).myReceivers should be(expected)
    }

    "pick none when alone" in {
      val ring = HeartbeatNodeRing(cc, Set(cc), Set.empty, 3)
      ring.myReceivers should be(Set())
    }

  }
}