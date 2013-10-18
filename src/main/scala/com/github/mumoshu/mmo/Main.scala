package org.mmo

import com.github.mumoshu.mmo.server.TCPIPServer

object Main extends App {
  TCPIPServer.createServer(port = 1234)
}
