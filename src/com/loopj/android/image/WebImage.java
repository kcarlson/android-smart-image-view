package com.loopj.android.image;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class WebImage implements SmartImage {
	private static final int CONNECT_TIMEOUT = 5000;
	private static final int READ_TIMEOUT = 10000;

	private static WebImageCache webImageCache;

	private static SSLSocketFactory sslSocketFactory;

	/**
	 * Default
	 */
	private static WebImageCacheKeyProvider webImageCacheKeyProvider = new WebImageCacheKeyProvider() {
		@Override
		public String getCacheKey(String url) {
			if (url == null) {
				throw new RuntimeException("Null url passed in");
			} else {
				return url.replaceAll("[.:/,%?&=]", "+")
						.replaceAll("[+]+", "+");
			}
		}

	};

	public static void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
		WebImage.sslSocketFactory = sslSocketFactory;
	}

	public static void setWebImageCacheKeyProvider(
			WebImageCacheKeyProvider webImageCacheKeyProvider) {
		WebImage.webImageCacheKeyProvider = webImageCacheKeyProvider;
	}

	private final String url;

	public WebImage(String url) {
		this.url = url;
	}

	public Bitmap getBitmap(Context context) {
		// Don't leak context
		if (webImageCache == null) {
			webImageCache = new WebImageCache(context, webImageCacheKeyProvider);
		}

		// Try getting bitmap from cache first
		Bitmap bitmap = null;
		if (url != null) {
			bitmap = webImageCache.get(url);
			if (bitmap == null) {
				bitmap = getBitmapFromUrl(url);
				if (bitmap != null) {
					webImageCache.put(url, bitmap);
				}
			}
		}

		return bitmap;
	}

	private Bitmap getBitmapFromUrl(String url) {
		Bitmap bitmap = null;

		try {
			URLConnection conn = new URL(url).openConnection();

			if (url.startsWith("https") && sslSocketFactory != null) {
				((HttpsURLConnection) conn)
						.setSSLSocketFactory(sslSocketFactory);
			}

			conn.setConnectTimeout(CONNECT_TIMEOUT);
			conn.setReadTimeout(READ_TIMEOUT);
			bitmap = BitmapFactory
					.decodeStream((InputStream) conn.getContent());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return bitmap;
	}

	public static void removeFromCache(String url) {
		if (webImageCache != null) {
			webImageCache.remove(url);
		}
	}
}
