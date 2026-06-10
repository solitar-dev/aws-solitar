export default defineNuxtConfig({
	modules: [
		"@unocss/nuxt",
		"@nuxt/fonts",
		"@nuxtjs/color-mode",
		"@vueuse/nuxt",
		"@regle/nuxt",
		"nuxt-og-image",
		"vue-sonner/nuxt",
		"@nuxtjs/i18n",
		"@solitar/ui/nuxt",
	],
	css: ["@solitar/assets/solitar.css", "~/assets/css/main.css"],
	srcDir: "src/app",
	dir: {
		public: "src/public",
	},
	components: [
		{
			path: "~/components",
			pathPrefix: false,
		},
	],
	fonts: {
		defaults: {
			formats: ["ttf"],
		},
		families: [
			{
				name: "Geist",
				weights: ["400", "500", "600", "700", "800"],
				global: true,
			},
			{
				name: "Geist Mono",
				weights: ["400", "500", "600", "700", "800"],
				global: true,
			},
			{
				name: "Orbitron",
				weights: ["400"],
				global: true,
			},
		],
	},
	// Internationalization
	i18n: {
		restructureDir: "./src/i18n",
		defaultLocale: "en",
		// no_prefix: locale lives in a cookie, no /vi/ path — keeps the
		// root /[shortCode] redirect route untouched.
		strategy: "no_prefix",
		locales: [
			{ code: "en", name: "English", file: "en.json" },
			{ code: "vi", name: "Tiếng Việt", file: "vi.json" },
		],
		// SSG-safe: browser detection bakes the payload at build time in one locale (broken under
		// static export). Locale is switched manually in LanguageSetting.vue via setLocale.
		detectBrowserLanguage: false,
	},
	// Runtime
	runtimeConfig: {
		public: {
			apiBaseUrl: "",
			site: {
				name: "",
				url: "",
				env: "",
			},
		},
	},
	// OG images: zero-runtime so Satori/Playwright stay out of the static bundle.
	ogImage: {
		zeroRuntime: true,
	},
	// Build: static export via `nuxt generate` -> .output/public (flat .html, autoSubfolderIndex
	// false). No SSR preset; the old aws-amplify preset is removed in the AWS-native migration.
	nitro: {
		prerender: {
			autoSubfolderIndex: false,
			// Don't let a flaky OG-image renderer abort the whole static build; social previews
			// degrade gracefully instead of failing the deploy.
			failOnError: false,
		},
	},

	// Development
	devtools: { enabled: true },
	compatibilityDate: "2025-07-15",
});
