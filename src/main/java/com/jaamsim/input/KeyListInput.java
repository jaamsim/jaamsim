/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.jaamsim.basicsim.Entity;

/**
 * Class KeyListInput for storing a list of entities of class V, with an optional key of class K1
 */
public class KeyListInput<K1 extends Entity, V extends Entity> extends ListInput<ArrayList<V>> {

	private Class<K1> keyClass;
	private Class<V> valClass;
	private HashMap<K1,ArrayList<V>> hashMap;
	private ArrayList<V> noKeyValue; // the value when there is no key

	public KeyListInput(Class<K1> kClass, Class<V> vClass, String keyword, String cat, ArrayList<V> def) {
		super(keyword, cat, def);
		keyClass = kClass;
		valClass = vClass;
		hashMap = new HashMap<>();
		noKeyValue = def;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void copyFrom(Input<?> in) {
		super.copyFrom(in);
		KeyListInput<K1, V> inp = (KeyListInput<K1, V>) in;
		hashMap = inp.hashMap;
		noKeyValue = inp.noKeyValue;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		for (KeywordIndex each : kw.getSubArgs())
			this.innerParse(each);
	}

	private void innerParse(KeywordIndex kw) {
		ArrayList<String> input = new ArrayList<>(kw.numArgs());
		for (int i = 0; i < kw.numArgs(); i++)
			input.add(kw.getArg(i));

		ArrayList<K1> list;
		try {
			// Determine the key(s)
			list = Input.parseEntityList(input.subList(0, 1), keyClass, true);
		}
		catch (InputErrorException e) {
			// A key was not provided.  Set the "no key" value

			// If adding to the list
			// The input is of the form: ++ <value1 value2 value3...>
			if( kw.getArg( 0 ).equals( "++" ) ) {

				ArrayList<V> newNoKeyValue;
				if( noKeyValue == null )
					newNoKeyValue = new ArrayList<>();
				else
					newNoKeyValue = new ArrayList<>( noKeyValue );

				ArrayList<V> addedValues = Input.parseEntityList( input.subList(1,input.size()), valClass, true );
				for( V val : addedValues ) {
					if( newNoKeyValue.contains( val ) )
						throw new InputErrorException(INP_ERR_NOTUNIQUE, val.getName());
					newNoKeyValue.add( val );
				}

				noKeyValue = newNoKeyValue;
			}
			// If removing from the list
			// The input is of the form: -- <value1 value2 value3...>
			else if( kw.getArg( 0 ).equals( "--" ) ) {

				ArrayList<V> removedValues = Input.parseEntityList( input.subList(1,input.size()), valClass, true );
				for( V val : removedValues ) {
					if( ! noKeyValue.contains( val ) )
						InputAgent.logWarning( "Could not remove " + val + " from " + this.getKeyword() );
					noKeyValue.remove( val );
				}
			}
			// Otherwise, just set the list normally
			// The input is of the form: <value1 value2 value3...>
			else {
				noKeyValue = Input.parseEntityList( input, valClass, true );
			}
			return;
		}

		// If adding to the list
		// The input is of the form: <Key> ++ <value1 value2 value3...>
		if( kw.getArg( 1 ).equals( "++" ) ) {

			// Set the value for the given keys
			for( int i = 0; i < list.size(); i++ ) {
				ArrayList<V> values;
				if( hashMap.get( list.get( i ) ) == null )
					values = new ArrayList<>();
				else
					values = new ArrayList<>( hashMap.get( list.get( i ) ) );

				ArrayList<V> addedValues = Input.parseEntityList( input.subList(2,input.size()), valClass, true );
				for( V val : addedValues ) {
					if( values.contains( val ) )
						throw new InputErrorException(INP_ERR_NOTUNIQUE, val.getName());
					values.add( val );
				}

				hashMap.put( list.get(i), values );
			}
		}
		// If removing from the list
		// The input is of the form: <Key> -- <value1 value2 value3...>
		else if( kw.getArg( 1 ).equals( "--" ) ) {

			// Set the value for the given keys
			for( int i = 0; i < list.size(); i++ ) {
				ArrayList<V> values = new ArrayList<>( hashMap.get( list.get( i ) ) );

				ArrayList<V> removedValues = Input.parseEntityList( input.subList(2,input.size()), valClass, true );
				for( V val : removedValues ) {
					if( ! values.contains( val ) )
						InputAgent.logWarning( "Could not remove " + val + " from " + this.getKeyword() );
					values.remove( val );
				}

				hashMap.put( list.get(i), values );
			}
		}
		// Otherwise, just set the list normally
		// The input is of the form: <Key> <value1 value2 value3...>
		else {
			// Determine the value
			ArrayList<V> val = Input.parseEntityList( input.subList(1,input.size()), valClass, true );

			// Set the value for the given keys
			for( int i = 0; i < list.size(); i++ ) {
				hashMap.put( list.get(i), val );
			}
		}
	}

	@Override
	public void setTokens(KeywordIndex kw) {
		isDef = false;

		String[] args = kw.getArgArray();

		// If there are previous inputs and existing or new keys
		if (args.length > 0) {
			if (valueTokens != null && hashMap.keySet().size() > 0) {
				this.appendTokens(args);
				return;
			}
		}

		// Consider the following input cases:
		// Object1 Keyword1 { Key1 ++ Entity1 ...
		// Object1 Keyword1 { { Key1 ++ Entity1 ...
		if (args.length >= 3) {
			if (args[1].equals( "++" ) || args[1].equals( "--" ) ||
				args[2].equals( "++" ) || args[2].equals( "--" )) {

				this.appendTokens(args);
				return;
			}
		}

		valueTokens = args;
	}

	@Override
	public ArrayList<V> getValue() {
		return null;
	}

	public ArrayList<V> getValueFor( K1 k1 ) {
		ArrayList<V> val = hashMap.get( k1 );
		if( val == null ) {
			return noKeyValue;
		}
		else {
			return val;
		}
	}

	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.isEmpty())
			return "";

		StringBuilder tmp = new StringBuilder(defValue.get(0).getName());
		for (int i = 1; i < defValue.size(); i++) {
			tmp.append(SEPARATOR);
			tmp.append(defValue.get(i).getName());
		}
		return tmp.toString();
	}

	@Override
	public void reset() {
		super.reset();
		hashMap.clear();
		noKeyValue = this.getDefaultValue();
	}

	@Override
	public int getListSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for(V each: Entity.getClonesOfIterator(valClass) ) {
			if(each.testFlag(Entity.FLAG_GENERATED))
				continue;

			list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public String toString() {
		return String.format("%s %s", noKeyValue, hashMap);
	}

	public ArrayList<K1> getAllKeys() {
		ArrayList<K1> keys = new ArrayList<>();

		for (K1 each : hashMap.keySet()) {
			keys.add(each);
		}

		return keys;
	}
}
