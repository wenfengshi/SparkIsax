/**
   Copyright [2011] [Josh Patterson]

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

 */

package cn.edu.fudan.mmdb.hbase.isax.index;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import cn.edu.fudan.mmdb.timeseries.TPoint;
import cn.edu.fudan.mmdb.timeseries.TSException;
import cn.edu.fudan.mmdb.timeseries.Timeseries;
import cn.edu.fudan.mmdb.isax.ISAXUtils;
import cn.edu.fudan.mmdb.isax.Sequence;
import cn.edu.fudan.mmdb.isax.index.IndexHashParams;
import cn.edu.fudan.mmdb.isax.index.NodeType;
import cn.edu.fudan.mmdb.isax.index.TerminalNode;
import cn.edu.fudan.mmdb.isax.index.TimeseriesInstance;

public class TestTerminalNodePersisted {
	@Test
	public void testSerDe() {
		IndexHashParams p = new IndexHashParams();
		p.base_card = 4;
		p.d = 1;
		p.isax_word_length = 4;
		p.orig_ts_len = 8;
		p.threshold = 100;

		Sequence s = new Sequence(8); // root node seqeunce, needs nothing more
										// than a word len

		TerminalNodePersisted n = new TerminalNodePersisted(s, p);
		// n.debug_helper();

		byte[] rep = n.getBytes();

		System.out.println("internal bytes: " + rep.length);

		Sequence key = new Sequence(0);
		key.parseFromIndexHash("1.1");

		TerminalNodePersisted n_d = new TerminalNodePersisted(key);

		n_d.deserialize(rep);

		n_d.key.setOrigLength(n_d.params.orig_ts_len);

		System.out.println("params: " + n_d.params.threshold + ", o-len: " + n_d.params.orig_ts_len);
		// System.out.println( "children: " + );
		// n_d.DebugKeys();

	}

	@Test
	public void testComplexSerDe() {

		Timeseries ts_1 = new Timeseries();

		ts_1.add(new TPoint(1.0, 0));
		ts_1.add(new TPoint(-0.5, 1));
		ts_1.add(new TPoint(-0.25, 2));
		ts_1.add(new TPoint(0.0, 3));

		ts_1.add(new TPoint(0.25, 4));
		ts_1.add(new TPoint(0.50, 5));
		ts_1.add(new TPoint(0.75, 6));
		ts_1.add(new TPoint(-2.0, 7));

		Timeseries ts_2 = new Timeseries();

		ts_2.add(new TPoint(1.0, 0));
		ts_2.add(new TPoint(-0.5, 1));
		ts_2.add(new TPoint(-0.25, 2));
		ts_2.add(new TPoint(0.0, 3));

		ts_2.add(new TPoint(0.25, 4));
		ts_2.add(new TPoint(0.50, 5));
		ts_2.add(new TPoint(0.75, 6));
		ts_2.add(new TPoint(-2.1, 7));

		Timeseries ts_3 = new Timeseries();

		ts_3.add(new TPoint(1.0, 0));
		ts_3.add(new TPoint(-0.5, 1));
		ts_3.add(new TPoint(-0.25, 2));
		ts_3.add(new TPoint(0.0, 3));

		ts_3.add(new TPoint(0.25, 4));
		ts_3.add(new TPoint(0.50, 5));
		ts_3.add(new TPoint(0.75, 6));
		ts_3.add(new TPoint(-1.9, 7));

		Sequence seq = null;
		try {
			seq = ISAXUtils.CreateiSAXSequence(ts_2, 4, 4);
		} catch (TSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// IndexHashParams params = new IndexHashParams();
		IndexHashParams p = new IndexHashParams();
		p.base_card = 4;
		p.d = 1;
		p.isax_word_length = 4;
		p.orig_ts_len = 8;
		p.threshold = 2;

		TerminalNodePersisted node = new TerminalNodePersisted(seq, p);

		TimeseriesInstance tsi_A = new TimeseriesInstance(ts_1);
		tsi_A.AddOccurence("foo.txt", 10);

		TimeseriesInstance tsi_B = new TimeseriesInstance(ts_2);
		tsi_B.AddOccurence("foo.txt", 1);

		TimeseriesInstance tsi_C = new TimeseriesInstance(ts_3);
		tsi_B.AddOccurence("foo.txt", 12);
		
		HBaseUtils.open();
		
		node.Insert(tsi_A);
		node.Insert(tsi_B);
		node.Insert(tsi_C);

		System.out.println(" size: " + node.arInstances.size());

		byte[] bytes_node = node.getBytes();

		System.out.println("serde size: " + bytes_node.length);

		TerminalNodePersisted serde_node = new TerminalNodePersisted(seq);

		serde_node.deserialize(bytes_node);

		serde_node.key.setOrigLength(serde_node.params.orig_ts_len);

		serde_node.DebugInstances();

		TimeseriesInstance ts_answer = serde_node.getNodeInstanceByKey(ts_2.toString());

		HashMap<String, Long> l1 = ts_answer.getOccurences();

		System.out.println("occurences: " + l1.size());

		HBaseUtils.close();
		 
		assertEquals("check instance exists", l1.size(), 2);

		// ArrayList<String> keys = (ArrayList<String>) l1.keySet();

	}

	@Test
	public void testApproxSearch() {
		Timeseries ts_1 = new Timeseries();

		ts_1.add(new TPoint(1.0, 0));
		ts_1.add(new TPoint(-0.5, 1));
		ts_1.add(new TPoint(-0.25, 2));
		ts_1.add(new TPoint(0.0, 3));

		ts_1.add(new TPoint(0.25, 4));
		ts_1.add(new TPoint(0.50, 5));
		ts_1.add(new TPoint(0.75, 6));
		ts_1.add(new TPoint(-2.0, 7));

		Timeseries ts_2 = new Timeseries();

		ts_2.add(new TPoint(1.0, 0));
		ts_2.add(new TPoint(-0.5, 1));
		ts_2.add(new TPoint(-0.25, 2));
		ts_2.add(new TPoint(0.0, 3));

		ts_2.add(new TPoint(0.25, 4));
		ts_2.add(new TPoint(0.50, 5));
		ts_2.add(new TPoint(0.75, 6));
		ts_2.add(new TPoint(-2.1, 7));

		Timeseries ts_3 = new Timeseries();

		ts_3.add(new TPoint(1.0, 0));
		ts_3.add(new TPoint(-0.5, 1));
		ts_3.add(new TPoint(-0.25, 2));
		ts_3.add(new TPoint(0.0, 3));

		ts_3.add(new TPoint(0.25, 4));
		ts_3.add(new TPoint(0.50, 5));
		ts_3.add(new TPoint(0.75, 6));
		ts_3.add(new TPoint(-1.9, 7));

		Sequence seq = null;
		try {
			seq = ISAXUtils.CreateiSAXSequence(ts_2, 4, 4);
		} catch (TSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// IndexHashParams params = new IndexHashParams();
		IndexHashParams p = new IndexHashParams();
		p.base_card = 4;
		p.d = 1;
		p.isax_word_length = 4;
		p.orig_ts_len = 8;
		p.threshold = 2;
		
		TerminalNodePersisted node = new TerminalNodePersisted(seq, p);

		TimeseriesInstance tsi_A = new TimeseriesInstance(ts_1);
		tsi_A.AddOccurence("foo.txt", 10);

		TimeseriesInstance tsi_B = new TimeseriesInstance(ts_2);
		tsi_B.AddOccurence("foo.txt", 1);

		TimeseriesInstance tsi_C = new TimeseriesInstance(ts_3);
		tsi_B.AddOccurence("foo.txt", 12);

		HBaseUtils.open();
		
		node.Insert(tsi_A);
		node.Insert(tsi_B);
		node.Insert(tsi_C);

		System.out.println(" size: " + node.arInstances.size());

		byte[] bytes_node = node.getBytes();

		System.out.println("serde size: " + bytes_node.length);

		Iterator i = node.getNodeInstancesIterator();

		while (i.hasNext()) {
			String strKey = i.next().toString();
			System.out.println("occur-key: " + strKey);

		}
		HBaseUtils.close();

	}
}