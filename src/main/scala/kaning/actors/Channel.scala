package kaning.actors

import scala.collection.mutable.Map
import akka.actor.{PoisonPill, Actor, ActorRef, Props}
import kaning.messages._
import kaning.messages.RegisteredClientList
import kaning.messages.ChatMessage
import kaning.messages.RegisterClientMessage
import kaning.messages.ChatInfo
import kaning.messages.PrivateMessage
import scala.concurrent.duration._
import kaning.actors.ChatServerActor.DeleteChannel

object Channel {
  def props(channelId: String) = Props(classOf[Channel], channelId)
}

class Channel(val channelId: String) extends Actor {
  import ChatServerActor.JoinedChannel
  import ChatServerActor.LeftChannel

  case object CheckForEmptyChannel

  def receive = usersConnected()

  def usersConnected(connectedClients: Map[User, ActorRef] = Map.empty): Receive = {
    case CheckForEmptyChannel =>
      if (connectedClients.isEmpty) context.parent ! DeleteChannel(channelId)

    case m @ ChatMessage(_, x: String) =>
      println(sender.path.name + ": " + x)
      // send this message to everyone in the room except the person who sent it
      connectedClients.values.filter(_ != sender).foreach(_.forward(m))

    case JoinedChannel(_, identity, clientRef) =>
      if(connectedClients.contains(identity)){
        clientRef ! ChatInfo(s"REGISTRATION FAILED: ${identity} is already registered")
      }else{
        clientRef ! ChatInfo("REGISTERED SUCCESSFULLY")
        connectedClients.values.filter(_ != clientRef).foreach(_ ! ChatInfo(s"${identity} join this channel"))
        context.become(usersConnected(connectedClients + (identity -> clientRef)))
      }

    case m @ PrivateMessage(target, _) =>
      connectedClients.values.filter(_.path.name.contains(target)).foreach(_.forward(m))

    case RegisteredClients =>
      println(s"${sender.path.name} requested for the room list")
      sender ! RegisteredClientList(connectedClients.keys)

    case LeftChannel(_, identity, clientRef) =>
      println(s"${identity} left this channel")
      // remove client from registered client set and send poison pill
      connectedClients.values.filter(_ != clientRef).foreach(_ ! ChatInfo(s"${identity} left this channel"))
      context.become(usersConnected(connectedClients - identity))
      context.system.scheduler.scheduleOnce(10 minutes, self, CheckForEmptyChannel)(context.dispatcher)

    case _ => sender ! ChatInfo("Stop mumbling and articulate, you're off protocol buddy")
  }
}
