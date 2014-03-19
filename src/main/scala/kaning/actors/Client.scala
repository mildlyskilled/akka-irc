package kaning.actors

import akka.actor._
import akka.remote.RemoteScope
import com.typesafe.config.ConfigFactory
import kaning.messages.Unregister
import kaning.messages.RegisteredClientList
import kaning.messages.ChatMessage
import kaning.messages.RegisterClientMessage
import kaning.messages.ChatInfo
import kaning.messages.PrivateMessage
import kaning.messages.RegisteredClients
import scala.tools.jline.console.ConsoleReader
import java.net.{NetworkInterface, InetAddress}
import scala.collection.JavaConversions._

object ChatClientApplication {

  def main(args:Array[String]) {


    def distillIpAddresses(interface:NetworkInterface):String = {
        interface.getInterfaceAddresses.filter(_.getBroadcast != null).head.getAddress.getHostAddress
    }

    println("Starting Akka Chat Client Actor")
    /* construct client with current machine's IP address instead of using the config value
    * get network interfaces make sure it's not loopbak (i.e. points outside of itself)
    * and filter for those that are actually "up"
    */
    val interfaces = new JEnumerationWrapper(NetworkInterface.getNetworkInterfaces).toList.filter(!_.isLoopback).filter(_.isUp)
    /** Ideally this should give a list of ip addresses and then we choose the one we want
    * but alas I am lazy so just pop the first ip address that works and use it instead
    * I use getBroadcast here as a subtle way of filtering out IPV6 addresses they have 
    * a null value
    */
    val ipAddresses = for( i <- interfaces) yield distillIpAddresses(i)
    
    /**
    * First make sure the ip address in the configuration file is not 
    * in the actual IP address list found on this machine
    */
    val clientconfig = ConfigFactory.load.getConfig("chatclient")
    val ipaddressinconfig = clientconfig.getString("akka.remote.netty.tcp.hostname")

    /**
    * If the ip address in our configuration is also in the ip addresses in this "distilled"
    * list just use that otherwise if we have more than one give options and if there's only
    * one IP address use that
    */
    val ipAddress = {
        if(!ipAddresses.contains(ipaddressinconfig)){
            if(ipAddresses.size > 1){
                ipAddresses foreach println
                var ip = ""
                /**
                * Make sure the IP address typed in here is valid otherwise this prompt
                * will be displayed FOREVER (unitl a keyboard interrupt of course)
                */
                while(!ipAddresses.contains(ip.trim)){
                    ip = new ConsoleReader().readLine("Which IP Address shall we bind to?  ")
                }
                ip.trim
            }else{
                ipAddresses.head
            }    
        }else{
            ipaddressinconfig
        }
        
    }

    // In some circles this would be the username
    val identity = new ConsoleReader().readLine("identify yourself: ")

    /**
    * Apply the ip address to the configuration we will be using to construct the client actor
    */
    val clientConfig = ConfigFactory.parseString(s"""akka.remote.netty.tcp.hostname="$ipAddress" """)
    val defaultConfig = ConfigFactory.load.getConfig("chatclient")
    val completeConfig = clientConfig.withFallback(defaultConfig)

    /* construct system using the complete config which is a result of "merging"
    * the parsed string and the default configs from the akka remote sub-system
    */
    val system = ActorSystem("AkkaChat", completeConfig)
    
    /*
    * get the server reference here because we will bind and forward messages to
    * it from our nifty console input
    */
    val serverconfig = ConfigFactory.load.getConfig("chatserver")
    val serverAddress = serverconfig.getString("akka.remote.netty.tcp.hostname")
    val serverPort = serverconfig.getString("akka.remote.netty.tcp.port")
    val serverPath = s"akka.tcp://AkkaChat@$serverAddress:$serverPort/user/chatserver"
    val server = system.actorSelection(serverPath) // <-- this is where we get the server reference

    // NOW CONSTRUCT THE CLIENT using as a member of the system defined above
    val client = system.actorOf(Props(classOf[ChatClientActor]), name = identity)
    
    // some input parsing logic to filter out private messages and so special things to it
    // like NOT Broadcast it to all connected clients
    val privateMessageRegex = """^@([^\s]+) (.*)$""".r
	
    // we can implement a help feature here to explain the protocol
    println("Type /join to join the chat room")

    /* Iterate infinitely over a stream created from our jline console reader object and 
    * use some functional concepts over this i.e. pattern matching takeWhile and the 
    * lovely foreach
    */
    Iterator.continually(new ConsoleReader().readLine("> ")).takeWhile(_ != "/exit").foreach { msg =>
      msg match {
        case "/list" =>
          server.tell(RegisteredClients, client)

        case "/join" =>
          server.tell(RegisterClientMessage(client, identity), client)

        case "/leave" => 
          server.tell(Unregister(identity), client)
        
        case "/offprotocol" => 
          server.tell("OFF PROTOCOL MESSAGE", client)
          
        case privateMessageRegex(target, msg) =>
          server.tell(PrivateMessage(target, msg), client)

        case _ =>
          server.tell(ChatMessage(msg), client)
      }
    }

    println("Exiting...")
    // Tell the server to remove us from currently connected clients
    server.tell(Unregister(identity), client)
    //@TODO find a graceful way to exit the application here

  }
}

class ChatClientActor  extends Actor {

    def receive = {

      case ChatMessage(message) =>
        println(s"${sender.path.name}: $message")

      case ChatInfo(msg) =>
        println ("INFO: ["+ msg +"]")

      case PrivateMessage(_, message) =>
        println(s"- ${sender.path.name}(PM): $message")

      case RegisteredClientList(list) =>
        for (x <- list) println(x)

      case _ => println("Client Received something")
   }
}
