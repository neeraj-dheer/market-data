package com.mywork.markets;

public class Tick {
	private final Instruments instrument;
	private final Side side;
	private final double bidPrice;
	private final double bidAmount;	
	private final double offerAmount;
	private final double offerPrice;
	
	public Tick(
			Instruments instrument, 
			Side side,
			double bidPrice, 
			double bidAmount,
			double offerPrice,
			double offerAmount
			) {
		this.instrument = instrument;
		this.side = side;
		this.bidPrice = bidPrice;
		this.offerAmount = offerAmount;
		this.offerPrice = offerPrice;
		this.bidAmount = bidAmount;
	}

	public Instruments getInstrument() {
		return instrument;
	}

	public Side getSide() {
		return side;
	}

	public double getBidPrice() {
		return bidPrice;
	}

	public double getOfferAmount() {
		return offerAmount;
	}

	public double getOfferPrice() {
		return offerPrice;
	}

	public double getBidAmount() {
		return bidAmount;
	}
	
	public String toString() {
		return "Instrument : [" + instrument + "] Side : [" + side + "]"
				+ " bidPrice: [" + bidPrice + "] bidAmount : [" + bidAmount + "]"
				+ " offerPrice: [" + offerPrice + "] offerAmount : [" + offerAmount + "]";
	}
}
