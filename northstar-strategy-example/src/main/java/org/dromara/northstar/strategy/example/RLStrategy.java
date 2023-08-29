package org.dromara.northstar.strategy.example;


import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.IModuleStrategyContext;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.slf4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * 本示例用于展示写一个策略的必要元素，以及最基本的开平仓操作、超时撤单操作
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent(RLStrategy.NAME)		// 该注解是用于给策略命名用的，所有的策略都要带上这个注解
public class RLStrategy implements TradeStrategy{
	
	protected static final String NAME = "示例-RL策略";	// 之所以要这样定义一个常量，是为了方便日志输出时可以带上策略名称
	
	private InitParams params;	// 策略的参数配置信息
	
	private IModuleStrategyContext ctx;		// 模组的操作上下文
	
	private JSONObject storeObj = new JSONObject(); 	// 可透视状态计算信息
	
	private Logger log;
	
	/**
	 * 定义该策略的参数。该类每个策略必须自己重写一个，类名必须为InitParams，必须继承DynamicParams，必须是个static类。
	 * @author KevinHuangwl
	 */
	public static class InitParams extends DynamicParams {			// 每个策略都要有一个用于定义初始化参数的内部类，类名称不能改
		
		@Setting(label="操作间隔", type = FieldType.NUMBER, order = 10, unit = "秒")		// Label注解用于定义属性的元信息。可以声明单位
		private int actionInterval;						// 属性可以为任意多个，当元素为多个时order值用于控制前端的显示顺序

	}
	
	/***************** 以下如果看不懂，基本可以照搬 *************************/
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		this.params = (InitParams) params;
	}
	
	@Override
	public void setContext(IModuleContext context) {
		ctx = context;
		log = ctx.getLogger();
	}
	
	@Override
	public JSONObject getStoreObject() {
		return storeObj;
	}

	@Override
	public void setStoreObject(JSONObject storeObj) {
		this.storeObj = storeObj;
	}
	/***************** 以上如果看不懂，基本可以照搬 *************************/

	private long nextActionTime;
	
	@Override
	public void onTick(TickField tick) {
		log.info("TICK触发: C:{} D:{} T:{} P:{} V:{} OI:{} OID:{}", 
				tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime(), 
				tick.getLastPrice(), tick.getVolume(), tick.getOpenInterest(), tick.getOpenInterestDelta());
    }

	@Override
	public void onMergedBar(BarField bar) {
		log.debug("策略每分钟触发");
		log.debug("{} K线数据： 开 [{}], 高 [{}], 低 [{}], 收 [{}]",
				bar.getUnifiedSymbol(), bar.getOpenPrice(), bar.getHighPrice(), bar.getLowPrice(), bar.getClosePrice());
		
		String url = "http://localhost:5001/get-action";
		JSONObject jsonData = new JSONObject();
		jsonData.put("open_price", bar.getOpenPrice());
		jsonData.put("high_price", bar.getHighPrice());
		jsonData.put("low_price", bar.getLowPrice());
		jsonData.put("close_price", bar.getClosePrice());
		String jsonContent = jsonData.toString();
        try (CloseableHttpClient HttpClient = HttpClients.createDefault()){
            HttpPost httpPost = new HttpPost(url);
            StringEntity entity = new StringEntity(jsonContent);
            httpPost.setEntity(entity);
            try (CloseableHttpResponse response = HttpClient.execute(httpPost)) {
				HttpEntity responseEntity = response.getEntity();
				if (responseEntity != null) {
					String jsonResponse = EntityUtils.toString(responseEntity);
					JSONObject jsonObject = JSON.parseObject(jsonResponse);
					Integer actionID = jsonObject.getInteger("action"); // 1: 买；0: 持仓；-1: 卖
					log.info("actionID: {}", actionID);
					
					
					switch (ctx.getState()) {
						case EMPTY -> {
							if (actionID == 1) {
								ctx.submitOrderReq(TradeIntent.builder()
										.contract(ctx.getContract(bar.getUnifiedSymbol()))
										.operation(SignalOperation.BUY_OPEN)
										.priceType(PriceType.OPP_PRICE)
										.volume(1)
										.timeout(5000)
										.build());
								log.info("多开");
							} else if (actionID == -1) {
								ctx.submitOrderReq(TradeIntent.builder()
										.contract(ctx.getContract(bar.getUnifiedSymbol()))
										.operation(SignalOperation.SELL_OPEN)
										.priceType(PriceType.OPP_PRICE)
										.volume(1)
										.timeout(5000)
										.build());
								log.info("空开");
							}
						}
						case HOLDING_LONG -> {
							if (actionID == -1) {
								ctx.submitOrderReq(TradeIntent.builder()
										.contract(ctx.getContract(bar.getUnifiedSymbol()))
										.operation(SignalOperation.SELL_CLOSE)
										.priceType(PriceType.OPP_PRICE)
										.volume(1)
										.timeout(5000)
										.build());
								log.info("平多");
							}
						}
						case HOLDING_SHORT -> {
							if (actionID == 1) {
								ctx.submitOrderReq(TradeIntent.builder()
										.contract(ctx.getContract(bar.getUnifiedSymbol()))
										.operation(SignalOperation.BUY_CLOSE)
										.priceType(PriceType.OPP_PRICE)
										.volume(1)
										.timeout(5000)
										.build());
								log.info("平空");
							}
						}
						default -> {
							log.info("当前状态：{}", ctx.getState());
						}
					}
				}
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	@Override
	public void onOrder(OrderField order) {
		// 委托单状态变动回调
	}

	@Override
	public void onTrade(TradeField trade) {
		// 成交回调
	}

}
