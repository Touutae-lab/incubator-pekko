/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.actor.typed.internal.routing

import java.util.concurrent.ThreadLocalRandom

import org.apache.pekko
import pekko.actor.Address
import pekko.actor.typed.ActorRef
import pekko.annotation.InternalApi
import pekko.routing.ConsistentHash

/**
 * Kept in the behavior, not shared between instances, meant to be stateful.
 *
 * INTERNAL API
 */
@InternalApi
sealed private[pekko] trait RoutingLogic[T] {

  def selectRoutee(msg: T): ActorRef[T]

  /**
   * Invoked an initial time before `selectRoutee` is ever called and then every time the set of available
   * routees changes.
   *
   * @param newRoutees The updated set of routees. For a group router this could be empty, in that case
   *                   `selectRoutee()` will not be called before `routeesUpdated` is invoked again with at
   *                   least one routee. For a pool the pool stops instead of ever calling `routeesUpdated`
   *                   with an empty list of routees.
   */
  def routeesUpdated(newRoutees: Set[ActorRef[T]]): Unit
}

/**
 * INTERNAL API
 */
@InternalApi
private[pekko] object RoutingLogics {

  final class RoundRobinLogic[T] extends RoutingLogic[T] {

    private var currentRoutees: Array[ActorRef[T]] = _

    private var nextIdx = 0

    def selectRoutee(msg: T): ActorRef[T] = {
      if (nextIdx >= currentRoutees.length) nextIdx = 0
      val selected = currentRoutees(nextIdx)
      nextIdx += 1
      selected
    }

    override def routeesUpdated(newRoutees: Set[ActorRef[T]]): Unit = {
      // make sure we keep a somewhat similar order so we can potentially continue roundrobining
      // from where we were unless the set of routees completely changed
      // Also, avoid putting all entries from the same node next to each other in case of cluster
      val sortedNewRoutees = newRoutees.toArray.sortBy(ref => (ref.path.toStringWithoutAddress, ref.path.address))

      if (currentRoutees ne null) {
        val firstDiffIndex = {
          var idx = 0
          while (idx < currentRoutees.length &&
            idx < sortedNewRoutees.length &&
            currentRoutees(idx) == sortedNewRoutees(idx)) {
            idx += 1
          }
          idx
        }
        if (nextIdx > firstDiffIndex) nextIdx -= 1
      }
      currentRoutees = sortedNewRoutees
    }
  }

  final class RandomLogic[T] extends RoutingLogic[T] {

    private var currentRoutees: Array[ActorRef[T]] = _

    override def selectRoutee(msg: T): ActorRef[T] = {
      val selectedIdx = ThreadLocalRandom.current().nextInt(currentRoutees.length)
      currentRoutees(selectedIdx)
    }

    override def routeesUpdated(newRoutees: Set[ActorRef[T]]): Unit = {
      currentRoutees = newRoutees.toArray
    }
  }

  final class ConsistentHashingLogic[T](virtualNodesFactor: Int, mapping: T => String, baseAddress: Address)
      extends RoutingLogic[T] {
    require(virtualNodesFactor > 0, "virtualNodesFactor has to be a positive integer")

    private var pathToRefs: Map[String, ActorRef[T]] = Map.empty

    private var consistentHash: ConsistentHash[String] = ConsistentHash(Set.empty, virtualNodesFactor)

    override def selectRoutee(msg: T): ActorRef[T] = pathToRefs(consistentHash.nodeFor(mapping(msg)))

    override def routeesUpdated(newRoutees: Set[ActorRef[T]]): Unit = {
      val updatedPathToRefs = newRoutees.map(routee => toFullAddressString(routee) -> routee).toMap
      val withoutOld = pathToRefs.keySet.diff(updatedPathToRefs.keySet).foldLeft(consistentHash)(_ :- _)
      consistentHash = updatedPathToRefs.keySet.diff(pathToRefs.keySet).foldLeft(withoutOld)(_ :+ _)
      pathToRefs = updatedPathToRefs
    }

    private def toFullAddressString(routee: ActorRef[T]): String = routee.path.address match {
      case Address(_, _, None, None) => routee.path.toStringWithAddress(baseAddress)
      case _                         => routee.path.toString
    }
  }

}
