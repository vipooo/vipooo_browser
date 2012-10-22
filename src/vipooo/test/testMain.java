package vipooo.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class testMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		URL url;
		HTMLEditorKit.ParserCallback parser_callback = new MenuParserCallback();
		HttpURLConnection conn = null;
		InputStreamReader in = null;
		BufferedReader reader = null;
		ParserDelegator pd = null;

		try {
			url =  new URL("http://menu.2ch.net/bbsmenu.html");
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setInstanceFollowRedirects(false);
			conn.addRequestProperty("User-Agent", "Monazilla/1.00");
			conn.addRequestProperty("Accept-Language", "ja");
			conn.getResponseCode();
			in = new InputStreamReader(conn.getInputStream(), "SJIS");
			reader = new BufferedReader(in);
			pd = new ParserDelegator();
			pd.parse(reader, parser_callback, true);
			in.close();
			reader.close();
			conn.disconnect();
			System.out.println(((MenuParserCallback) parser_callback).getBoard_map());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

class MenuParserCallback extends HTMLEditorKit.ParserCallback {
	private boolean start_bold = false;
	private boolean parse_2ch_start_flag = false;
	private boolean start_category_flag = false;
	private String start_trigger_text = "2ch総合案内";
	private ArrayList<String> ignore_text = new ArrayList<String>();
	private ArrayList<String> ignore_category = new ArrayList<String>();
	private String board_category_name = "";
	private String next_board_url = "";
	private LinkedHashMap<String, String> board_map = new LinkedHashMap<String, String>();

	public MenuParserCallback() {
		// 無視カテゴリ
		ignore_category.add("おすすめ");
		ignore_category.add("チャット");
		ignore_category.add("２ｃｈ＠ＩＲＣ");
		ignore_category.add("運営案内");
		ignore_category.add("ガイドライン");
		ignore_category.add("2chメルマガ");
		ignore_category.add("ツール類");
		ignore_category.add("BBSPINK");
		ignore_category.add("まちＢＢＳ");
		ignore_category.add("他のサイト");

		// 無視板(2ch標準の板じゃないやつら)
		ignore_text.add("2ch検索");
		ignore_text.add("be.2ch.net");
		ignore_text.add("アンケート");
		ignore_text.add("2chビューア");
		ignore_text.add("2chオークション");
		ignore_text.add("2ch観察帳");
		ignore_text.add("2chメルマガ");
	}

	@Override
	public void handleStartTag(HTML.Tag tag, MutableAttributeSet attr, int pos) {
		// <B>が出たら次の地の文はカテゴリ名。なのでフラグを立てる。
		if (tag.equals(HTML.Tag.B)) {
			start_bold = true;
			start_category_flag = true;
		}
		// <A>タグが出たらurlを保持。あとで板名とペアにする。
		if (tag.equals(HTML.Tag.A)) {
			// 頭についてる広告タグは無視。
			if (parse_2ch_start_flag) {
				String href = (String) attr.getAttribute(HTML.Attribute.HREF);
				next_board_url = href;
			}
		}
	}

	// </B>タグ。次の地の文はカテゴリじゃないのでフラグを倒す。
	@Override
	public void handleEndTag(HTML.Tag tag, int pos) {
		if (tag.equals(HTML.Tag.B)) {
			start_bold = false;
		}
	}

	// 地の文。
	public void handleText(char[] data, int pos) {
		String text = new String(data);
		// カテゴリ名なら無視リストに入ってないかチェックしてからmapに追加。
		if (start_bold) {
			for (String s : ignore_category) {
				if (text.equals(s)) {
					start_category_flag = false;
					continue;
				}
			}
			if (start_category_flag) {
				board_category_name = text;
				board_map.put(text, "c");
			}
			// パース作業の開始フラグ
		} else if (text.equals(start_trigger_text)) {
			parse_2ch_start_flag = true;
			// カテゴリが無視リストに入ってないなら(板名,URL)をmapに登録
		} else if (parse_2ch_start_flag && start_category_flag) {
			Pattern pattern = Pattern.compile(".*\\.2ch\\.net/.*");
			Matcher matcher = pattern.matcher(next_board_url);
			boolean blnMatch = matcher.matches();
			for (String s : ignore_text) {
				if (text.equals(s)) {
					blnMatch = false;
					continue;
				}
			}
			if (blnMatch) {
				board_map.put(text, next_board_url);
			}
			next_board_url = "";
		}
	}

	public Map<String, String> getBoard_map() {
		return board_map;
	}

	public void sendRes(URL target_url, String submit_data) {
		try {
			URLConnection con = target_url.openConnection();
			con.setDoOutput(true);
			OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream());
			BufferedWriter bw = new BufferedWriter(osw);
			bw.write(submit_data);
			bw.close();
			osw.close();

			InputStreamReader isr = new InputStreamReader(con.getInputStream());
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
			br.close();
			isr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}