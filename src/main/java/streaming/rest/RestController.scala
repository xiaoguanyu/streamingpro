package streaming.rest

import java.util.concurrent.atomic.AtomicInteger

import net.csdn.annotation.rest.At
import net.csdn.modules.http.ApplicationController
import net.csdn.modules.http.RestRequest.Method._
import net.sf.json.JSONObject
import serviceframework.dispatcher.StrategyDispatcher
import streaming.core.strategy.platform.{PlatformManager, SparkStreamingRuntime}

/**
 * 4/30/16 WilliamZhu(allwefantasy@gmail.com)
 */
class RestController extends ApplicationController {
  @At(path = Array("/runtime/stop"), types = Array(GET))
  def stopRuntime = {
    runtime.destroyRuntime(true)
    render(200, "ok")
  }

  @At(path = Array("/job/add"), types = Array(POST))
  def addJob = {
    if (!runtime.isInstanceOf[SparkStreamingRuntime]) render(400, "only support Spark Streaming")
    val _runtime = runtime.asInstanceOf[SparkStreamingRuntime]
    val waitCounter = new AtomicInteger(0)
    while (!_runtime.streamingRuntimeInfo.sparkStreamingOperator.isStreamingCanStop()
      && waitCounter.get() < paramAsInt("waitRound", 1000)) {
      Thread.sleep(50)
      waitCounter.incrementAndGet()
    }
    dispatcher.createStrategy(param("name"), JSONObject.fromObject(request.contentAsString()))
    if (_runtime.streamingRuntimeInfo.sparkStreamingOperator.isStreamingCanStop()) {
      _runtime.destroyRuntime(false)
      platformManager.run(null, true)
      render(200, "ok")
    } else {
      render(400, "timeout")
    }

  }

  def platformManager = PlatformManager.getOrCreate

  def dispatcher = StrategyDispatcher.getOrCreate()

  def runtime = PlatformManager.getRuntime(null, new java.util.HashMap[Any, Any]())
}
