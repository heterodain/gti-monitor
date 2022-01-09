package com.heterodain.gtimonitor.device;

import java.io.IOException;

import org.springframework.stereotype.Component;

import lombok.var;
import lombok.extern.slf4j.Slf4j;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;

/**
 * グリッドタイインバーターデバイス
 */
@Component
@Slf4j
public class GridTieInverterDevice {
	/** シリアルポート名 */
	private SerialConnection serial;
	/** ModbusのユニットID */
	private Integer unitId;

	/**
	 * 接続
	 * 
	 * @param serialPortName シリアルポート名
	 * @param unitId         ユニットID
	 * @throws IOException
	 */
	public void connect(String serialPortName, Integer unitId) throws IOException {
		log.info("GTI(port={}, unitId={})に接続します...", serialPortName, unitId);

		this.unitId = unitId;

		var params = new SerialParameters();
		params.setPortName(serialPortName);
		params.setBaudRate(9600);
		params.setDatabits(8);
		params.setParity("None");
		params.setStopbits(1);
		params.setEncoding("rtu");
		params.setEcho(false);

		serial = new SerialConnection(params);
		serial.open();
	}

	/**
	 * 現在の発電電力取得
	 * 
	 * @return 発電電力(W)
	 * @throws IOException
	 * @throws ModbusException
	 */
	public Double getCurrentPower() throws IOException, ModbusException {
		try {
			var req = new ReadMultipleRegistersRequest(86, 1);
			req.setUnitID(unitId);
			var tr = new ModbusSerialTransaction(serial);
			tr.setRequest(req);
			tr.execute();

			var res = (ReadMultipleRegistersResponse) tr.getResponse();
			return ((double) res.getRegisterValue(0)) / 10D;

		} catch (ModbusException e) {
			serial.close();
			serial.open();
			throw e;
		}
	}

	/**
	 * 切断
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (serial != null && serial.isOpen()) {
			log.info("GTI(port={}, unitId={})を切断します...", serial.getPortName(), unitId);
			serial.close();
		}
	}
}