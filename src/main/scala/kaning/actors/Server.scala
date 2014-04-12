package kaning.actors

import akka.actor.{Actor, Props, ActorSystem, ActorRef, PoisonPill}
import collection.mutable.Map
import kaning.messages._
import kaning.actors._
import com.typesafe.config.ConfigFactory
import akka.persistence._
import scala.concurrent.duration._

object ChatServerApplication extends App {
  println("Starting Akka Chat Server Actor")
  val system = ActorSystem("AkkaChat", ConfigFactory.load.getConfig("chatserver"))
  val server = system.actorOf(Props[ChatServerActor], name = "chatserver")
  server ! StartUp
}

object ChatServerActor {
  sealed trait ChatServerCommand
  case class CreateChannel(channelId: String, user: User) extends ChatServerCommand
  case class JoinChannel(channelId: String, user: User) extends ChatServerCommand
  case class LeaveChannel(channelId: String, user: User) extends ChatServerCommand
  case class UserLogin(user: User) extends ChatServerCommand
  case class UserLeave(user: User) extends ChatServerCommand
  case class DeleteChannel(channelId: String) extends ChatServerCommand
  case object ListChannels extends ChatServerCommand

  sealed trait ChatServerEvent
  case class CreatedChannel(channelId: String) extends ChatServerEvent
  case class JoinedChannel(channelId: String, user: User, sender: ActorRef) extends ChatServerEvent
  case class LeftChannel(channelId: String, user: User, sender: ActorRef) extends ChatServerEvent
  case class UserLoggedIn(user: User) extends ChatServerEvent
  case class UserLeft(user: User) extends ChatServerEvent
  case class DeletedChannel(channelId: String) extends ChatServerEvent
}

class ChatServerActor extends EventsourcedProcessor {
  case class ChatServerState(users: Seq[User], channels: Map[String, ActorRef])
  case object TakeSnapshot

  import ChatServerActor._

  var channels = Map.empty[String, ActorRef]
  var users = Seq.empty[User]

  context.system.scheduler.schedule(10 minutes, 10 minutes, self, TakeSnapshot)(context.dispatcher)

  def updateState(evt: ChatServerEvent) = evt match {
    case CreatedChannel(channelId) =>
      val act = context.actorOf(Channel.props(channelId))
      channels = channels + (channelId -> act)
      println(s"Channel created: $channelId")
    case msg@JoinedChannel(channelId, user, s) =>
      channels.get(channelId).foreach(_.tell(msg, s))
      println(s"User $user joined channel $channelId")
    case msg@LeftChannel(channelId, user, s) =>
      channels.get(channelId).foreach(_.tell(msg, s))
      println(s"User $user left channel $channelId")
    case UserLoggedIn(user) =>
      users = users :+ user
      println(s"User $user logged in")
    case UserLeft(user) =>
      users = users.filter(_ != user)
      println(s"User $user logged out")
    case DeletedChannel(channelId) =>
      // TODO: Try using sender here!
      channels.get(channelId).foreach(_ ! PoisonPill)
      channels = channels - channelId
      println(s"Channel $channelId deleted")
    case evt => sys.error(s"Unrecognized event: $evt")
  }

  val receiveRecover: Receive = {
    case evt: ChatServerEvent => updateState(evt)
    case SnapshotOffer(_, ChatServerState(u, c)) =>
      channels = c
      users = u
  }

  def receiveCommand: Receive = {
    // TODO: Validate users and commands and everything
    case CreateChannel(channelId, user) =>
      persist(CreatedChannel(channelId))(updateState)
      persist(JoinedChannel(channelId, user, sender))(updateState)
    case JoinChannel(channelId, user) =>
      persist(JoinedChannel(channelId: String, user, sender))(updateState)
    case LeaveChannel(channelId, user) =>
      persist(LeftChannel(channelId: String, user, sender))(updateState)
    case UserLogin(user) =>
      persist(UserLoggedIn(user))(updateState)
    case UserLeave(user) =>
      persist(UserLeft(user))(updateState)
    case DeleteChannel(channelId) =>
      persist(DeletedChannel(channelId))(updateState)
    case ListChannels =>
      sender ! ChannelList(channels.keys)
    case m@ChatMessage(channel, msg) =>
      channels.get(channel).foreach(_.forward(m))
    case TakeSnapshot  => saveSnapshot(ChatServerState(users, channels))
  }
}
