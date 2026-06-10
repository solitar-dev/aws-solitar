declare module "*.vue" {
	import type { defineComponent } from "vue";

	const component: ReturnType<typeof defineComponent>;
	export default component;
}

// Side-effect CSS imports (e.g. Storybook preview: "@solitar/assets/solitar.css", "virtual:uno.css").
declare module "*.css";
