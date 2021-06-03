package tech.xuanwu.northstar.gateway.sim;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.DateTimeConstant;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
class GwOrderHolder {
	
	private String gatewayId;
	
	private Map<String, ContractField> contractMap;
	
	private int ticksOfCommission;
	
	private ConcurrentHashMap<String, OrderField> orderIdMap = new ConcurrentHashMap<>(100);
	private ConcurrentHashMap<TradeField, OrderField> doneOrderMap = new ConcurrentHashMap<>();
	
	private volatile String tradingDay = "";
	
	public GwOrderHolder (String gatewayId, int ticksOfCommission, Map<String, ContractField> contractMap) {
		this.gatewayId = gatewayId;
		this.ticksOfCommission = ticksOfCommission;
		this.contractMap = contractMap;
	}
	
	private OrderField.Builder makeOrder(SubmitOrderReqField submitOrderReq){
		String orderId = gatewayId + "_" + UUID.randomUUID().toString();
		String originOrderId = submitOrderReq.getOriginOrderId();
		OrderField.Builder ob = OrderField.newBuilder();
		ob.setActiveTime(String.valueOf(System.currentTimeMillis()));
		ob.setOrderId(orderId);
		ob.setContract(submitOrderReq.getContract());
		ob.setPrice(submitOrderReq.getPrice());
		ob.setDirection(submitOrderReq.getDirection());
		ob.setOriginOrderId(originOrderId);
		ob.setGatewayId(gatewayId);
		ob.setVolumeCondition(submitOrderReq.getVolumeCondition());
		ob.setTradingDay(tradingDay);
		ob.setOrderDate(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		ob.setOrderTime(LocalTime.now().format(DateTimeConstant.T_FORMAT_FORMATTER));
		ob.setAccountId(gatewayId);
		ob.setTotalVolume(submitOrderReq.getVolume());
		ob.setOffsetFlag(submitOrderReq.getOffsetFlag());
		ob.setOrderPriceType(submitOrderReq.getOrderPriceType());
		ob.setGtdDate(submitOrderReq.getGtdDate());
		ob.setMinVolume(submitOrderReq.getMinVolume());
		ob.setStopPrice(submitOrderReq.getStopPrice());
		ob.setSequenceNo("1");
		ob.setOrderStatus(OrderStatusEnum.OS_Touched);
		ob.setStatusMsg("报单已提交");
		
		return ob;
	}
	
	protected OrderField tryOrder(SubmitOrderReqField submitOrderReq, AccountField af) {
		OrderField.Builder ob = makeOrder(submitOrderReq);
		ContractField contract = submitOrderReq.getContract();
		double marginRate = submitOrderReq.getDirection() == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		int vol = submitOrderReq.getVolume();
		double price = submitOrderReq.getPrice();
		double cost = vol * price * contract.getMultiplier() * marginRate + contract.getPriceTick() * ticksOfCommission;
		if(cost > af.getAvailable()) {
			ob.setOrderStatus(OrderStatusEnum.OS_Rejected);
			ob.setStatusMsg("资金不足");
			log.warn("资金不足，无法下单");
			return ob.build();
		}
		
		OrderField of = ob.build();
		orderIdMap.put(of.getOrderId(), of);
		log.info("成功下单：{}", of.toString());
		return of;
	}
	
	protected OrderField tryOrder(SubmitOrderReqField submitOrderReq, PositionField pf) {
		OrderField.Builder ob = makeOrder(submitOrderReq);
		if(pf == null) {
			ob.setOrderStatus(OrderStatusEnum.OS_Rejected);
			ob.setStatusMsg("仓位不足");
			log.warn("仓位不足，无法下单");
			return ob.build();
		}
		int totalAvailable = pf.getPosition() - pf.getFrozen();
		int tdAvailable = pf.getTdPosition() - pf.getTdFrozen();
		int ydAvailable = pf.getYdPosition() - pf.getYdFrozen();
		if(submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_CloseToday && tdAvailable < submitOrderReq.getVolume()
				|| submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_CloseYesterday && ydAvailable < submitOrderReq.getVolume()
				|| totalAvailable < submitOrderReq.getVolume()) {
			ob.setOrderStatus(OrderStatusEnum.OS_Rejected);
			ob.setStatusMsg("仓位不足");
			log.warn("仓位不足，无法下单");
			return ob.build();
		}
		
		OrderField of = ob.build();
		orderIdMap.put(of.getOrderId(), of);
		log.info("成功下单：{}", of.toString());
		return of;
	}
	
	protected OrderField cancelOrder(CancelOrderReqField cancelOrderReq) {
		boolean hasOrderId = cancelOrderReq.getOrderId() != null && orderIdMap.containsKey(cancelOrderReq.getOrderId());
		if(!hasOrderId) {
			return null;
		}
		
		OrderField order = orderIdMap.remove(cancelOrderReq.getOrderId());
		if(order.getOrderStatus() == OrderStatusEnum.OS_AllTraded) {
			return order;
		}
		
		OrderField.Builder ob = order.toBuilder();
		ob.setOrderStatus(OrderStatusEnum.OS_Canceled);
		ob.setCancelTime(LocalDateTime.now().format(DateTimeConstant.DT_FORMAT_FORMATTER));
		return ob.build();
	}
	
	protected List<TradeField> tryDeal(TickField tick) {
		tradingDay = tick.getTradingDay();
		final String unifiedSymbol = tick.getUnifiedSymbol();
		List<TradeField> tradeList = new ArrayList<>();
		orderIdMap.forEach((k, order) -> {
			boolean untrade = order.getOrderStatus() != OrderStatusEnum.OS_AllTraded 
					&& order.getOrderStatus() != OrderStatusEnum.OS_Canceled
					&& order.getOrderStatus() != OrderStatusEnum.OS_Rejected;
			if(StringUtils.equals(order.getContract().getUnifiedSymbol(), unifiedSymbol) && untrade) {
				// TODO 该模拟撮合逻辑属于简单实现，没有考虑价格深度
				if(order.getDirection() == DirectionEnum.D_Buy && tick.getAskPrice(0) <= order.getPrice()
						|| order.getDirection() == DirectionEnum.D_Sell && tick.getBidPrice(0) >= order.getPrice()) {
					// 修改挂单状态
					OrderField.Builder ob = order.toBuilder();
					ob.setTradedVolume(ob.getTotalVolume());
					ob.setOrderStatus(OrderStatusEnum.OS_AllTraded);
					ob.setStatusMsg("挂单全部成交");
					
					OrderField of = ob.build();
					
					ContractField contract = contractMap.get(unifiedSymbol);
					// 计算成交
					TradeField trade = TradeField.newBuilder()
							.setTradeId(System.currentTimeMillis()+"")
							.setAccountId(gatewayId)
							.setAdapterOrderId("")
							.setContract(contract)
							.setDirection(order.getDirection())
							.setGatewayId(gatewayId)
							.setHedgeFlag(order.getHedgeFlag())
							.setOffsetFlag(order.getOffsetFlag())
							.setOrderId(order.getOrderId())
							.setOriginOrderId(order.getOriginOrderId())
							.setPrice(order.getPrice())
							.setPriceSource(PriceSourceEnum.PSRC_LastPrice)
							.setTradeDate(tradingDay)
							.setTradeTime(LocalTime.now().format(DateTimeConstant.T_FORMAT_FORMATTER))
							.setVolume(order.getTotalVolume())
							.build();
					
					tradeList.add(trade);
					doneOrderMap.put(trade, of);
					orderIdMap.remove(of.getOrderId());
				}
			}
		});
		
		return tradeList;
	}
	
	protected OrderField confirmWith(TradeField trade) {
		return doneOrderMap.remove(trade);
	}
	
	protected double getFrozenMargin() {
		double totalFrozenAmount = 0;
		Double r1 = orderIdMap.reduce(100, 
			(k, v) -> (v.getTotalVolume() - v.getTradedVolume()) * v.getContract().getMultiplier() * v.getPrice() * v.getContract().getLongMarginRatio(),
			(a, b) -> a + b);
		totalFrozenAmount += r1 == null ? 0 : r1.doubleValue();
		return totalFrozenAmount;
	}
	
	protected void proceedDailySettlement() {
		
	}
}