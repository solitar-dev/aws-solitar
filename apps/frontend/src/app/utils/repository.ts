import type { $Fetch, NitroFetchRequest } from "nitropack";

type StatisticData = {
	totalLinks: number;
	totalClicks: number;
};

type UrlShortenerResponse = {
	originalUrl: string;
	shortCode: string;
};

export type UrlShortenerBody = {
	url: string;
	alias?: string;
	password?: string;
	expireTime?: string;
};

export const repository = <T>(fetch: $Fetch<T, NitroFetchRequest>) => ({
	async getStatisticData(): Promise<StatisticData> {
		return fetch<StatisticData>("/statistics");
	},
	async shortenUrl(body: UrlShortenerBody): Promise<UrlShortenerResponse> {
		return fetch<UrlShortenerResponse>("/create", {
			method: "post",
			body,
		});
	},
});
