package naivebayes;
import java.util.List;

public class Document {

	private List<String> tokens;
	private List<String> topics;
	private Integer sentenceId;
	
	public Document(List<String> tokens, List<String> topics, Integer sentenceId) {
		super();
		this.tokens = tokens;
		this.topics = topics;
		this.sentenceId = sentenceId;
	}
	
	public List<String> getTokens() {
		return tokens;
	}
	public void setTokens(List<String> tokens) {
		this.tokens = tokens;
	}
	public List<String> getTopics() {
		return topics;
	}
	public void setTopics(List<String> topics) {
		this.topics = topics;
	}
	public Integer getSentenceId() {
		return sentenceId;
	}
	public void setSentenceId(Integer sentenceId) {
		this.sentenceId = sentenceId;
	}

}
