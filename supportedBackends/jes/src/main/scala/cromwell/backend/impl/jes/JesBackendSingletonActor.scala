package cromwell.backend.impl.jes

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import cromwell.backend.AbortWorkflow
import cromwell.core.Dispatcher.BackendDispatcher
import cromwell.backend.impl.jes.statuspolling.JesApiQueryManager
import cromwell.backend.impl.jes.statuspolling.JesApiQueryManager.JesApiQueryManagerRequest
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive

final case class JesBackendSingletonActor(qps: Int Refined Positive, serviceRegistryActor: ActorRef) extends Actor with ActorLogging {

  val jesApiQueryManager = context.actorOf(JesApiQueryManager.props(qps, serviceRegistryActor))

  override def receive = {
    case apiQuery: JesApiQueryManagerRequest =>
      log.debug("Forwarding API query to PAPI query manager actor")
      jesApiQueryManager.forward(apiQuery)
    case abort: AbortWorkflow =>
      log.debug(s"Forwarding abort request for workflow ${abort.workflowId} to PAPI query manager actor")
      jesApiQueryManager.forward(abort)
  }
}

object JesBackendSingletonActor {
  def props(qps: Int Refined Positive, serviceRegistryActor: ActorRef): Props = Props(JesBackendSingletonActor(qps, serviceRegistryActor)).withDispatcher(BackendDispatcher)
}
