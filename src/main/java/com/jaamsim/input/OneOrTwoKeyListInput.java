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
import com.jaamsim.basicsim.Group;
/**
 * Class OneOrTwoKeyInput for storing objects of class V (e.g. Double or DoubleVector),
 * with one mandatory key of class K1 and one optional key of class K2
 */
public class OneOrTwoKeyListInput<K1 extends Entity, K2 extends Entity, V extends Entity> extends ListInput<ArrayList<V>> {

	private Class<K1> key1Class;
	private Class<K2> key2Class;
	private Class<V> valClass;
	private HashMap<K1,HashMap<K2,ArrayList<V>>> hashMap;
	private ArrayList<V> noKeyValue; // the value when there is no key

	public OneOrTwoKeyListInput(Class<K1> k1Class, Class<K2> k2Class, Class<V> vClass, String keyword, String cat, ArrayList<V> def) {
		super(keyword, cat, def);
		key1Class = k1Class;
		key2Class = k2Class;
		valClass = vClass;
		hashMap = new HashMap<>();
		noKeyValue = def;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void copyFrom(Input<?> in) {
		super.copyFrom(in);
		OneOrTwoKeyListInput<K1, K2, V> inp = (OneOrTwoKeyListInput<K1, K2, V>) in;
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

		// If one entity key is not provided, set the default value
		Entity ent1 = Input.tryParseEntity( input.get( 0 ), key1Class );

		// could be a group
		if( ent1 == null ) {
			ent1 = Input.tryParseEntity( input.get( 0 ), Group.class );
		}

		if( ent1 == null ) {
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

		// Determine the keys
		ArrayList<K1> list = Input.parseEntityList(input.subList(0, 1), key1Class, true);
		ArrayList<K2> list2;

		// If ent2 is null, assume the line is of the form <Key1> <Value>
		// The second key was not given.  Use null as the second key.
		int numKeys;
		Entity ent2 = Input.tryParseEntity( input.get( 1 ), key2Class );

		// could be a group
		if( ent2 == null ) {
			ent2 = Input.tryParseEntity( input.get( 1 ), Group.class );
		}

		if( ent2 == null ) {
			numKeys = 1;
			list2 = new ArrayList<>();
			list2.add( null );
		}
		else {
			// Otherwise assume the line is of the form <Key1> <Key2> <Value>
			// The second key was given.  Store the second key.
			numKeys = 2;
			list2 = Input.parseEntityList(input.subList(1, 2), key2Class, true);
		}

		// If adding to the list
		// The input is of the form: <Key1> ++ <value1 value2 value3...>
		//                       or: <Key1> <Key2> ++ <value1 value2 value3...>
		if( kw.getArg( numKeys ).equals( "++" ) ) {

			// Set the value for the given keys
			for( int i = 0; i < list.size(); i++ ) {
				HashMap<K2,ArrayList<V>> h1 = hashMap.get( list.get( i ) );
				for( int j = 0; j < list2.size(); j++ ) {
					ArrayList<V> values;
					if( h1.get( list2.get( j ) ) == null )
						values = new ArrayList<>();
					else
						values = new ArrayList<>( h1.get( list2.get( j ) ) );

					ArrayList<V> addedValues = Input.parseEntityList( input.subList(numKeys+1,input.size()), valClass, true );
					for( V val : addedValues ) {
						if( values.contains( val ) )
							throw new InputErrorException(INP_ERR_NOTUNIQUE, val.getName());
						values.add( val );
					}
					h1.put( list2.get(j), values );
				}
			}
		}
		// If removing from the list
		// The input is of the form: <Key1> -- <value1 value2 value3...>
		//                       or: <Key1> <Key2> -- <value1 value2 value3...>
		else if( kw.getArg( numKeys ).equals( "--" ) ) {

			// Set the value for the given keys
			for( int i = 0; i < list.size(); i++ ) {
				HashMap<K2,ArrayList<V>> h1 = hashMap.get( list.get( i ) );
				for( int j = 0; j < list2.size(); j++ ) {
					ArrayList<V> values = new ArrayList<>( h1.get( list2.get( j ) ) );

					ArrayList<V> removedValues = Input.parseEntityList( input.subList(numKeys+1,input.size()), valClass, true );
					for( V val : removedValues ) {
						if( ! values.contains( val ) )
							InputAgent.logWarning( "Could not remove " + val + " from " + this.getKeyword() );
						values.remove( val );
					}
					h1.put( list2.get(j), values );
				}
			}
		}
		// Otherwise, just set the list normally
		else {
			// Determine the value
			ArrayList<V> val = Input.parseEntityList( input.subList(numKeys,input.size()), valClass, true );

			// Set the value for the given keys
			for( int i = 0; i < list.size(); i++ ) {
				HashMap<K2,ArrayList<V>> h1 = hashMap.get( list.get( i ) );
				if( h1 == null ) {
					h1 = new HashMap<>();
					hashMap.put( list.get( i ), h1 );
				}
				for( int j = 0; j < list2.size(); j++ ) {
					h1.put( list2.get(j), val );
				}
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
		// Object1 Keyword1 { Key1 Key2 ++ Entity1 ...
		if (args.length >= 3 && ! args[0].equals( "{" )) {
			if (args[1].equals( "++" ) || args[1].equals( "--" ) ||
				args[2].equals( "++" ) || args[2].equals( "--" )) {

				this.appendTokens(args);
				return;
			}
		}

		// Consider the following input cases:
		// Object1 Keyword1 { { Key1 ++ Entity1 ...
		// Object1 Keyword1 { { Key1 Key2 ++ Entity1 ...
		if (args.length >= 4 && args[0].equals( "{") ) {
			if (args[2].equals( "++" ) || args[2].equals( "--" ) ||
				args[3].equals( "++" ) || args[3].equals( "--" )) {

				this.appendTokens(args);
				return;
			}
		}

		valueTokens = args;
	}

	public int size() {
		return hashMap.size();
	}

	@Override
	public ArrayList<V> getValue() {
		return null;
	}

	public ArrayList<V> getValueFor( K1 k1, K2 k2 ) {
		HashMap<K2,ArrayList<V>> h1 = hashMap.get( k1 );

		// Is k1 not in the table?
		if( h1 == null ) {
			return noKeyValue;
		}
		else {
			ArrayList<V> val = h1.get( k2 );

			// Is k2 not in the table?
			if( val == null ) {

				// Is null not in the table?
				ArrayList<V> val2 = h1.get( null );
				if( val2 == null ) {

					// Return the default value
					return noKeyValue;
				}
				else {

					// Return the value for (k1,null) i.e. when k1 is specified alone
					return val2;
				}
			}
			else {

				// Return the value for (k1,k2)
				return val;
			}
		}
	}

	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.isEmpty())
			return "";

		StringBuilder tmp = new StringBuilder(defValue.get(0).getName());
		for (int i = 1; i < defValue.size(); i ++) {
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
}
