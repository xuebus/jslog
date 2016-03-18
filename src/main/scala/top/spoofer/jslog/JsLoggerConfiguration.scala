/*
 * Copyright (c) 2015, dgl, Inc. All Rights Reserved.
 *
 * Author@ dgl
 *
 * jslog的配置模块, 配置模块提供3个选项分别是：
 * 1、日志信息刷写的时间间隔, 单位秒
 * 2、日志缓存队列的大小, 最小4个
 * 3、日志缓存的地方
 */

package top.spoofer.jslog

sealed class JsLoggerConfiguration {
  private var _flushInterval = 6      //日志信息刷写的时间间隔, 单位秒
  private var _cacheThreshold = 4    //日志缓存队列的大小, 最小4个
  private var _savePath = "./runlogs"  //日志缓存的地方
  private var _FSYNC_WAL = false       //强制将数据写入磁盘
  private var _appName = ""

  def flushInterval: Int = this._flushInterval
  def flushInterval_=(interval: Int): Boolean = {
    if (interval > 0) {
      this._flushInterval = interval
      true
    } else throw new Exception("flush Interval must more than 0")
  }

  def cacheThreshold: Int = this._cacheThreshold
  def cacheThreshold_=(threshold: Int): Boolean = {
    if (threshold >= 4) {
      this._cacheThreshold = threshold
      true
    } else throw new Exception("cache Threshold must more than 4")
  }

  def FSYNC_WAL: Boolean = this._FSYNC_WAL
  def FSYNC_WAL_=(wal: Boolean) = this._FSYNC_WAL = wal

  def appName: String = this._appName
  def appName_=(name: String) = this._appName = name

  def savePath: String = this._savePath
  def savePath_=(path: String) = if (checkPath(path)) this._savePath = path

  private def checkPath(path: String): Boolean = {
    if (path != null && path != "" && path != Unit.toString) true else false
  }
}