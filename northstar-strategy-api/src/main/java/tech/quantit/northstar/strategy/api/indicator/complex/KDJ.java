package tech.quantit.northstar.strategy.api.indicator.complex;

import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import xyz.redtorch.pb.CoreField.BarField;

import java.util.function.Function;

import static tech.quantit.northstar.strategy.api.indicator.function.AverageFunctions.SMA;
import static tech.quantit.northstar.strategy.api.indicator.function.StatsFunctions.HHV;
import static tech.quantit.northstar.strategy.api.indicator.function.StatsFunctions.LLV;

/**
 * N,M1,M2为KDJ指标参数
 * RSV:=(CLOSE-LLV(LOW,N))/(HHV(HIGH,N)-LLV(LOW,N))*100;//收盘价与N周期最低值做差，N周期最高值与N周期最低值做差，两差之间做比值。
 * K:SMA(RSV,M1,1);//RSV的M1日移动平均值，1为权重
 * D:SMA(K,M2,1);//K的M1日移动平均值，1为权重
 * J:3*K-2*D;
 */
public class KDJ {

	private int n;
	private int m1;
	private int m2;

	/**
	 * 创建KDJ指标线生成器
	 * @param n	RSV的计算周期
	 * @param m1 K的计算周期
	 * @param m2 D的计算周期
	 */
	public KDJ(int n, int m1, int m2) {
		this.n = n;
		this.m1 = m1;
		this.m2 = m2;
	}
	
	/**
	 * 创建KDJ指标线生成器
	 * @param n	RSV的计算周期
	 * @param m1 K的计算周期
	 * @param m2 D的计算周期
	 * @return
	 */
	public static KDJ of(int n, int m1, int m2) {
		return new KDJ(n, m1, m2);
	}

	
	/**
	 * 获取K值计算函数
	 * RSV:=(CLOSE-LLV(LOW,N))/(HHV(HIGH,N)-LLV(LOW,N))*100;//收盘价与N周期最低值做差，N周期最高值与N周期最低值做差，两差之间做比值。
	 * K=SMA(RSV,M1,1);//RSV的M1日移动平均值，1为权重
	 * @return
	 */
	public Function<BarField, TimeSeriesValue> k() {
		final TimeSeriesUnaryOperator llv = LLV(this.n);
		final TimeSeriesUnaryOperator hhv = HHV(this.n);
		final TimeSeriesUnaryOperator sma = SMA(this.m1, 1);
		return bar -> {
			TimeSeriesValue lowV = llv.apply(new TimeSeriesValue(bar.getLowPrice(), bar.getActionTimestamp()));
			TimeSeriesValue highV = hhv.apply(new TimeSeriesValue(bar.getHighPrice(), bar.getActionTimestamp()));
			double rsv = (bar.getClosePrice() - lowV.getValue()) / (highV.getValue() - lowV.getValue()) * 100;
			return sma.apply(new TimeSeriesValue(rsv, bar.getActionTimestamp()));
		};
	}

	/**
	 * 获取D值计算函数
	 * D=SMA(K,M2,1);//K的M1日移动平均值，1为权重
	 * @return
	 */
	public Function<BarField, TimeSeriesValue> d() {
		final Function<BarField, TimeSeriesValue> k = k();
		final TimeSeriesUnaryOperator sma = SMA(this.m2, 1);
		return bar -> sma.apply(k.apply(bar));
	}

	/**
	 * 获取J值计算函数
	 * J=3*K-2*D;
	 * @return
	 */
	public Function<BarField, TimeSeriesValue> j() {
		final Function<BarField, TimeSeriesValue> k = k();
		final Function<BarField, TimeSeriesValue> d = d();
		return bar -> {
			TimeSeriesValue v = k.apply(bar);
			TimeSeriesValue v0 = d.apply(bar);
			double j = 3 * v.getValue() - 2 * v0.getValue();
			return new TimeSeriesValue(j, v.getTimestamp());
		};
	}
}