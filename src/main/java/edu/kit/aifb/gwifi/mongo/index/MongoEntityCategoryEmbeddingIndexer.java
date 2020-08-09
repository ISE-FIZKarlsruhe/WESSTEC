package edu.kit.aifb.gwifi.mongo.index;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * This class is responsible to build the collection named "EntityEmbeddingIndex_**" and "CategoryEmbeddingIndex_**" in
 * MongoDB
 * 
 */
public class MongoEntityCategoryEmbeddingIndexer {

	private DBCollection dbEntityEmbeddingCollection;
	private DBCollection dbCategoryEmbeddingCollection;
	
	private BulkWriteOperation entityEmbeddingBuilder;
	private BulkWriteOperation categoryEmbeddingBuilder;

	private BufferedReader listReader;
	private BufferedReader vectorReader;

	public MongoEntityCategoryEmbeddingIndexer(String langLabel, String listFile, String vectorFile) throws Exception {
		Language lang = Language.getLanguage(langLabel);
		dbEntityEmbeddingCollection = MongoResource.INSTANCE.getDB()
				.getCollection(DBConstants.ENTITY_EMBEDDING_COLLECTION + lang.getLabel());
		dbCategoryEmbeddingCollection = MongoResource.INSTANCE.getDB()
				.getCollection(DBConstants.CATEGORY_EMBEDDING_COLLECTION + lang.getLabel());

		entityEmbeddingBuilder = dbEntityEmbeddingCollection.initializeUnorderedBulkOperation();
		categoryEmbeddingBuilder = dbCategoryEmbeddingCollection.initializeUnorderedBulkOperation();
		
		listReader = new BufferedReader(new FileReader(new File(listFile)));
		vectorReader = new BufferedReader(new FileReader(new File(vectorFile)));
	}

	// "configs/configuration_gwifi.properties" "en",
	// "new_c_e_train_neg10size400min_count1.list", "new_c_e_train_neg10size400min_count1.embedding"
	public static void main(String[] args) {
		try {
			String configPath = args[0];
			Property.setProperties(configPath);
			MongoEntityCategoryEmbeddingIndexer indexer = new MongoEntityCategoryEmbeddingIndexer(args[1], args[2],
					args[3]);
			indexer.insertData(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertData(boolean bulk) throws IOException {
		int num = 0, numEnt = 0, numCate = 0;
		String list, embedding;
		while (((list = listReader.readLine()) != null) && ((embedding = vectorReader.readLine()) != null)) {
			String name = list.substring(2).replace("_", " ");

			ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
			DataOutputStream tdos = new DataOutputStream(baos);

			String[] weights = embedding.split(" ");
			for (int i = 0; i < weights.length; i++) {
				tdos.writeInt(i);
				tdos.writeDouble(Double.valueOf(weights[i]));
			}

			ByteArrayOutputStream dbvector = new ByteArrayOutputStream();
			DataOutputStream dbdis = new DataOutputStream(dbvector);
			dbdis.writeInt(weights.length); // vector size
			dbdis.flush();
			dbvector.write(baos.toByteArray());
			dbvector.flush();
			dbdis.close();

			if (list.startsWith("e_")) {
				BasicDBObject item = new BasicDBObject(DBConstants.ENTITY_EMBEDDING_NAME, name);
				item.put(DBConstants.ENTITY_EMBEDDING_VECTOR, dbvector.toByteArray());
				if(bulk) {
					entityEmbeddingBuilder.insert(item);
				} else {
					dbEntityEmbeddingCollection.insert(item);
				}
				numEnt++;
				num++;
			} else if (list.startsWith("c_")) {
				BasicDBObject item = new BasicDBObject(DBConstants.CATEGORY_EMBEDDING_NAME, name);
				item.put(DBConstants.CATEGORY_EMBEDDING_VECTOR, dbvector.toByteArray());
				if(bulk) {
					categoryEmbeddingBuilder.insert(item);
				} else {
					dbCategoryEmbeddingCollection.insert(item);
				}
				numCate++;
				num++;
			}

			if(num/100000 == 0) {
				System.out.println(num + " items have been processed.");
				System.out.println(numEnt + " entities have been processed.");
				System.out.println(numCate + " categories have been processed.\n");
			}
			
			tdos.close();
			baos.close();
		}

		if(bulk) {
			entityEmbeddingBuilder.execute();
			categoryEmbeddingBuilder.execute();
		}
		
		if(num/100000 == 0) {
			System.out.println("In total, " + num + " items have been processed.");
			System.out.println("In total, " + numEnt + " entities have been processed.");
			System.out.println("In total, " + numCate + " categories have been processed.\n");
		}
		
		dbEntityEmbeddingCollection.createIndex(new BasicDBObject(DBConstants.ENTITY_EMBEDDING_NAME, 1));
		dbCategoryEmbeddingCollection.createIndex(new BasicDBObject(DBConstants.CATEGORY_EMBEDDING_NAME, 1));
	}

}
