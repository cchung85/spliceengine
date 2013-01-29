package com.splicemachine.derby.utils;

import com.splicemachine.derby.impl.sql.execute.ValueRow;
import org.apache.derby.iapi.error.StandardException;
import org.apache.derby.iapi.services.io.ArrayUtil;
import org.apache.derby.iapi.services.io.FormatableBitSet;
import org.apache.derby.iapi.sql.execute.ExecRow;
import org.apache.derby.iapi.types.DataValueDescriptor;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Serializable version of an ExecRow.
 *
 * @author Scott Fines
 * Created: Jan 29, 2013
 */
public class SerializingExecRow implements ExecRow, Externalizable  {
	private static final long serialVersionUID=1l;
	private ExecRow delegate;

	/**
	 * Used only for serialization, do not use!
	 */
	@Deprecated
	public SerializingExecRow() { }

	public SerializingExecRow(ExecRow delegate) {
		this.delegate = delegate;
	}

	@Override public ExecRow getClone() { return delegate.getClone(); }
	@Override public ExecRow getClone(FormatableBitSet clonedCols) { return delegate.getClone(clonedCols); }
	@Override public ExecRow getNewNullRow() { return delegate.getNewNullRow(); }
	@Override public void resetRowArray() { delegate.resetRowArray(); }
	@Override public DataValueDescriptor cloneColumn(int columnPosition) { return delegate.cloneColumn(columnPosition); }
	@Override public DataValueDescriptor[] getRowArrayClone() { return delegate.getRowArrayClone(); }
	@Override public DataValueDescriptor[] getRowArray() { return delegate.getRowArray(); }
	@Override public void setRowArray(DataValueDescriptor[] rowArray) { delegate.setRowArray(rowArray); }
	@Override public void getNewObjectArray() { delegate.getNewObjectArray(); }
	@Override public int nColumns() { return delegate.nColumns(); }
	@Override public DataValueDescriptor getColumn(int position) throws StandardException { return delegate.getColumn(position); }
	@Override public void setColumn(int position, DataValueDescriptor value) { delegate.setColumn(position, value); }

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		//write the total number of columns
		out.writeInt(nColumns());

		/*
		 * Write the DataValueDescriptors.
		 *
		 * Because some DataValueDescriptors don't serialize unless they are non-null,
		 * this block sparsely-writes only the non-null Descriptors using the format
		 * < total number of columns>
		 * < number of non-null columns>
		 * 	for each non-null Descriptor:
		 *    < position of the descriptor>
		 *    < value of the descriptor>
		 */
		DataValueDescriptor[] dvds = getRowArray();
		Map<DataValueDescriptor,Integer> nonNullEntries = new HashMap<DataValueDescriptor,Integer>(dvds.length);
		for(int pos=0;pos<dvds.length;pos++){
			DataValueDescriptor dvd = dvds[pos];
			if(dvd!=null&&!dvd.isNull()){
				nonNullEntries.put(dvd,pos);
			}
		}
		out.writeInt(nonNullEntries.size());
		for(DataValueDescriptor dvd:nonNullEntries.keySet()){
			out.writeInt(nonNullEntries.get(dvd));
			out.writeObject(dvd);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		delegate = new ValueRow(in.readInt());
		int nonNull = in.readInt();
		for(int i=0;i<nonNull;i++){
			int pos = in.readInt();
			DataValueDescriptor dvd = (DataValueDescriptor)in.readObject();
			delegate.getRowArray()[pos] = dvd;
		}
	}
}