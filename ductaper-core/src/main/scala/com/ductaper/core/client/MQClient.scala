package com.ductaper.core.client

import com.ductaper.core.message.MessageProps
import com.ductaper.core.route.BrokerRoutingData
import com.ductaper.core.serialization.MessageConverter

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, TimeUnit}
import scala.util.Try

/**
  * Created by zahari on 20/02/2017.
  */
trait MQClient {
  def send[T](data: T, routingData: BrokerRoutingData, messageProps: MessageProps)(implicit converter: MessageConverter): Unit
  def sendAndReceive[T,R](data:T, routingData: BrokerRoutingData, messageProps: MessageProps,timeout: Duration)(implicit converter: MessageConverter,responseManifest:Manifest[R]): Future[Try[R]]
  }