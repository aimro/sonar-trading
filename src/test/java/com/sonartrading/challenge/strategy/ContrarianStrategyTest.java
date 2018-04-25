package com.sonartrading.challenge.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.sonartrading.challenge.data.model.MakerSide;
import com.sonartrading.challenge.data.model.Trade;
import com.sonartrading.challenge.data.observer.TradesObserver.ObserverType;

import javafx.embed.swing.JFXPanel;

public class ContrarianStrategyTest {

	private Trade imaginaryTrade;
	private ContrarianStrategy contrarianStrategy;

	@Before
	public void setUp() throws Exception {
		imaginaryTrade = null;
		contrarianStrategy = new ContrarianStrategy();
	}

	@Test
	public void testTradesUpdatedUptick() {

		generateImaginaryTrade(Arrays.asList(trade(3), trade(2), trade(1), trade(0)));

		assertEquals(MakerSide.BUY, imaginaryTrade.getMakerSide());
		assertEquals(new Double(1), imaginaryTrade.getAmount());
		assertEquals(new Double(3), imaginaryTrade.getPrice());
	}

	@Test
	public void testTradesUpdatedDowntick() {
		generateImaginaryTrade(Arrays.asList(trade(0), trade(1), trade(2), trade(3)));

		assertEquals(MakerSide.SELL, imaginaryTrade.getMakerSide());
		assertEquals(new Double(1), imaginaryTrade.getAmount());
		assertEquals(new Double(0), imaginaryTrade.getPrice());
	}

	@Test
	public void testGetObserverType() {
		assertEquals(ObserverType.LOGIC, contrarianStrategy.getObserverType());
	}

	private void generateImaginaryTrade(List<Trade> trades) {

		CountDownLatch lock = new CountDownLatch(1);
		ImaginaryTradesObserver tradesObserver = new ImaginaryTradesObserver() {

			@Override
			public void newImaginaryTrade(Trade trade) {
				imaginaryTrade = trade;
				lock.countDown();
			}
		};

		contrarianStrategy.addImaginaryTradesObserver(tradesObserver);
		contrarianStrategy.tradesUpdated(trades);

		try {
			lock.await(2000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			fail("ContrarianStrategy did not return any result on time");
		}
	}

	private Trade trade(double value) {
		return new Trade(0l, MakerSide.BUY, null, 1.0, value);
	}

	@SuppressWarnings("unused")
	private JFXPanel fxPanel = new JFXPanel();

}
