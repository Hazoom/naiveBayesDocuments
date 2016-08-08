package naivebayes;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NaiveBayesIml {

	private List<Document> devDocuments;
	private List<Document> testDocuments;
	private Map<String,Integer> mapTopics;
	private Map<String,Double> mapProbabilitesCi;
	private Map<String,Map<String,Double>> mapProbabiliesXi;
	
	/**
	 * Main method of the class. Here we process the input files, calculate probabilities, predict
	 * topics of given data sets and evaluate our model.
	 * @param devfilename
	 * @param testfilename
	 * @param outputfilename
	 */
	public NaiveBayesIml(String devfilename, String testfilename, String outputfilename) {
		
		// Initialize
		this.devDocuments = new ArrayList<Document>();
		this.testDocuments = new ArrayList<Document>();
		this.mapTopics = new HashMap<String,Integer>();
		this.mapProbabilitesCi = new HashMap<String,Double>();
		this.mapProbabiliesXi = new HashMap<String,Map<String,Double>>();
		
		// Parse input files
		this.parseInputFile(devfilename, this.devDocuments);
		this.parseInputFile(testfilename, this.testDocuments );
		
		// Calculate probabilities
		this.calculateProbabilites(this.devDocuments);
		
		// Predictions on dev
		List<String> devPredictions = this.predict(this.devDocuments);
		
		// Predictions on test
		List<String> testPredictions = this.predict(this.testDocuments);
		
		// Dev accuracy
		double devAccuracy = this.calculateAccuracy(devPredictions, this.devDocuments);
		
		// Test accuracy
		double testAccuracy = this.calculateAccuracy(testPredictions, this.testDocuments);
		
		System.out.println("Train Accuracy = " + devAccuracy);
		System.out.println("Test Accuracy = " + testAccuracy);
	}
	
	public List<Document> getDevDocuments() {
		return this.devDocuments;
	}
	
	public List<Document> getTestDocuments() {
		return this.testDocuments;
	}
	
	/**
	 * Method parses input files and saves the documents into the memory
	 * @param filename
	 * @param documents
	 */
	private void parseInputFile(String filename, List<Document> documents) {
		
		BufferedReader br = null;
		
		try {
			br = new BufferedReader(new FileReader(filename));
			String line = null;
			int count = 0;
			Integer sentenceId = null;
			String[] words;
			List<String> topics = null;
			List<String> tokens;
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty()) {
					if (count % 2 == 0) { // header
						words = line.split("\t");
						for (String word : words) {
							word = word.trim();
						}
						sentenceId = Integer.valueOf(words[1]);
						topics = new ArrayList<String>();
						for (int i = 2; i < words.length; i++) {
							if (i == words.length - 1) { // Remove last character
								words[i] = words[i].substring(0, words[i].length() - 1);
							}
							topics.add(words[i]);
							
							// Update the count of each topic
							if (this.mapTopics.containsKey(words[i])) {
								Integer value = this.mapTopics.get(words[i]);
								value++;
								this.mapTopics.put(words[i], value);
							} else {
								this.mapTopics.put(words[i], Integer.valueOf(1));
							}
						}
					} else { // Body
						words = line.split(" ");
						tokens = new ArrayList<String>();
						for (String word : words) {
							tokens.add(word);
						}
						
						Document document = new Document(tokens, topics, sentenceId);
						documents.add(document);
					}
					count++;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Method calculates probabilities of P(X|Ci) and P(Ci)
	 * @param documents
	 */
	private void calculateProbabilites(List<Document> documents) {
		
		// Total number of topics seen
		int totalTopics = 0;
		for (Map.Entry<String,Integer> topic : this.mapTopics.entrySet()) {
			totalTopics += topic.getValue();
		}
		
		// P(Ci) probabilites
		for (Map.Entry<String,Integer> topic : this.mapTopics.entrySet()) {
			double prob_ci = (double)topic.getValue() / (double)totalTopics;
			this.mapProbabilitesCi.put(topic.getKey(), prob_ci);
		}
		
		// Calculate P(xi | Ci)
		for (Document document : documents) { // Run over all documents
			List<String> tokens = document.getTokens();
			
			for (String token : tokens) { // Run over all tokens in document
				if (this.mapProbabiliesXi.containsKey(token)) {
					Map<String,Double> mapXiTopics = this.mapProbabiliesXi.get(token);
					
					for (String topic : document.getTopics()) {
						
						if (mapXiTopics.containsKey(topic)) {
							Double value = mapXiTopics.get(topic);
							value = value + 1.0;
							mapXiTopics.put(topic, value);
						} else {
							mapXiTopics.put(topic, 1.0);
						}
					}
				} else { // Not contains
					Map<String,Double> mapXiTopics = new HashMap<String,Double>();
					
					for (String topic : document.getTopics()) {
						mapXiTopics.put(topic, 1.0);
					}
					
					this.mapProbabiliesXi.put(token, mapXiTopics);
				}
			}
		}
		
		
		// Divide by the number of tokens
		double smooth = 1.0;
		for (Map.Entry<String,Map<String,Double>> wordEntry : this.mapProbabiliesXi.entrySet()) {
			Map<String,Double> mapTopics = wordEntry.getValue();
			
			for (Map.Entry<String, Double> topicEntry : mapTopics.entrySet()) {
				Double totalTopicCount = Double.valueOf(this.mapTopics.get(topicEntry.getKey()));
				topicEntry.setValue((double)(topicEntry.getValue()+smooth) / totalTopicCount);
			}
		}
	}
	
	/**
	 * Method predicts a set of documents
	 * @param documents
	 * @return
	 */
	public List<String> predict(List<Document> documents) {
		List<String> predictions = new ArrayList<String>();
		
		for (Document document : documents) {
			String prediction = this.predict(document);
			predictions.add(prediction);
		}
		
		return predictions;
	}
	
	/**
	 * Method predicts a given document to its most relevant topic
	 * @param document
	 * @return
	 */
	public String predict(Document document) {
		
		Map<String,Double> probabiliesCi = new HashMap<String,Double>();
		
		double count = 1.0;
		double epsilon = 0.01;
		for (Map.Entry<String,Integer> topicEntry : this.mapTopics.entrySet()) {
			String topic = topicEntry.getKey();
			
			// Run over all the tokens in sentence
			List<String> tokens = document.getTokens();
			count = 1.0;
			for (String token : tokens) {
				
				// The word has seen before in the dataset
				if (this.mapProbabiliesXi.containsKey(token)) {
					// The word has seen with this topic
					if (this.mapProbabiliesXi.get(token).containsKey(topic)) {
						count = count*this.mapProbabiliesXi.get(token).get(topic);
					} else { // This word never seen with this topic
						count = count*(1.0/this.mapTopics.get(topic));
					}
				} else { // The word has never been in the dataset
					count = count*epsilon;
				}
			}
			
			// P(x|Ci)*P(Ci)
			count = count*this.mapProbabilitesCi.get(topic);
			
			probabiliesCi.put(topic, count);
		}
		double MaxProb = -1;
		String maxTopic = "";
		for (Map.Entry<String, Double> probEntry : probabiliesCi.entrySet()) {
			if (probEntry.getValue() > MaxProb) {
				MaxProb = probEntry.getValue();
				maxTopic = probEntry.getKey();
			}
		}
		
		return maxTopic;
	}
	
	/**
	 * Method calculates accuracy of a given set of documents and predictions
	 * @param predictions
	 * @param documents
	 * @return
	 */
	public double calculateAccuracy(List<String> predictions, List<Document> documents) {
		double good = 0.0;
		double bad = 0.0;
		
		int n = documents.size();
		
		for (int i = 0; i < n; i++) {
			String prediction = predictions.get(i);
			if (documents.get(i).getTopics().contains(prediction)) {
				good++;
			} else {
				bad++;
			}
		}
		
		double accuracy = good/(good+bad);
		
		return (int)(accuracy*1000)/1000.0;
	}
}
