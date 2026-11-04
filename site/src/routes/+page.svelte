<svelte:head>
  <title>pom-cli | Manipulate pom.xml from CLI</title>
  <meta name="description" content="Manipulate Maven pom.xml files from the command line." />
</svelte:head>

<div class="bg-slate-950 text-slate-200 min-h-screen">

  <!-- NAVBAR -->
  <header class="border-b border-slate-800">
    <div class="max-w-6xl mx-auto px-6 py-5 flex justify-between items-center">
      <div class="text-xl font-bold tracking-tight text-purple-400">
        pom-cli
      </div>

      <div class="flex gap-6 text-sm text-slate-400">
        <a href="#features" class="hover:text-white transition">Features</a>
        <a href="#install" class="hover:text-white transition">Install</a>
        <a href="https://github.com/yourrepo/pom-cli" target="_blank" class="hover:text-white transition">
          GitHub
        </a>
      </div>
    </div>
  </header>

  <!-- HERO -->
  <section class="max-w-6xl mx-auto px-6 py-24 grid md:grid-cols-2 gap-12 items-center">

    <div>
      <h1 class="text-4xl md:text-5xl font-extrabold leading-tight">
        Manipulate <span class="text-purple-400">pom.xml</span><br />
        From the Command Line
      </h1>

      <p class="mt-6 text-lg text-slate-400">
        Add, remove, upgrade, inspect, and refactor Maven POM files
        without opening your IDE.
      </p>

      <div class="mt-8 flex gap-4">
        <a href="#install"
           class="bg-purple-600 hover:bg-purple-500 px-6 py-3 rounded-xl font-medium shadow-lg shadow-purple-900/30 transition">
          Install Now
        </a>

        <a href="https://github.com/andirady/pom-cli"
           target="_blank"
           class="border border-slate-700 hover:border-slate-500 px-6 py-3 rounded-xl transition">
          View on GitHub
        </a>
      </div>
    </div>

    <!-- Terminal Preview -->
    <div class="bg-slate-900 border border-slate-800 rounded-2xl p-6 font-mono text-sm shadow-xl">
      <div class="text-emerald-400 mb-4">$ mkdir myapp && cd $_</div>
      <div class="text-emerald-400 mb-4">$ pom parent \
      <br>&nbsp;&nbsp;org.springframework.boot:spring-boot-starter-parent \
      <br>&nbsp;&nbsp;add spring-boot-starter-web</div>
      <div class="text-emerald-400">🔨 org.springframework.boot:spring-boot-starter-web:<i>&lt;managed&gt;</i> added</div>
    </div>
  </section>

  <!-- FEATURES -->
  <section id="features" class="border-t border-slate-800 py-24">
    <div class="max-w-6xl mx-auto px-6">
      <h2 class="text-3xl font-bold mb-12 text-center">
        Designed for Developers
      </h2>

      <div class="grid md:grid-cols-3 gap-10">

        <div class="bg-slate-900 border border-slate-800 p-6 rounded-2xl">
          <h3 class="text-xl font-semibold mb-4 text-purple-400">
            Dependency Management
          </h3>
          <ul class="text-slate-400 space-y-2 text-sm">
            <li>• Add / remove dependencies</li>
            <li>• Smart version alignment</li>
            <li>• Multi-module support</li>
          </ul>
        </div>

        <div class="bg-slate-900 border border-slate-800 p-6 rounded-2xl">
          <h3 class="text-xl font-semibold mb-4 text-purple-400">
            Query & Inspect
          </h3>
          <ul class="text-slate-400 space-y-2 text-sm">
            <li>• Extract POM values</li>
            <li>• Dependency tree view</li>
            <li>• JSON/YAML output</li>
          </ul>
        </div>

        <div class="bg-slate-900 border border-slate-800 p-6 rounded-2xl">
          <h3 class="text-xl font-semibold mb-4 text-purple-400">
            Safe & Deterministic
          </h3>
          <ul class="text-slate-400 space-y-2 text-sm">
            <li>• Preserves formatting</li>
            <li>• Schema validation</li>
            <li>• Dry-run mode</li>
          </ul>
        </div>

      </div>
    </div>
  </section>

  <!-- WHY SECTION -->
  <section class="border-t border-slate-800 py-24 bg-slate-900/40">
    <div class="max-w-4xl mx-auto px-6 text-center">
      <h2 class="text-3xl font-bold mb-8">
        Why pom-cli?
      </h2>

      <div class="grid md:grid-cols-2 gap-8 text-left">

        <div class="bg-slate-900 border border-slate-800 p-6 rounded-xl">
          <h4 class="font-semibold mb-3 text-slate-300">Traditional Way</h4>
          <ul class="text-slate-500 text-sm space-y-2">
            <li>• Open IDE</li>
            <li>• Manually edit XML</li>
            <li>• Risk formatting issues</li>
            <li>• Hard to automate</li>
          </ul>
        </div>

        <div class="bg-slate-900 border border-purple-600 p-6 rounded-xl">
          <h4 class="font-semibold mb-3 text-purple-400">pom-cli Way</h4>
          <ul class="text-slate-300 text-sm space-y-2">
            <li>• Stay in terminal</li>
            <li>• Declarative commands</li>
            <li>• Safe rewrite engine</li>
            <li>• Perfect for CI pipelines</li>
          </ul>
        </div>

      </div>
    </div>
  </section>

  <!-- INSTALL -->
  <section id="install" class="border-t border-slate-800 py-24">
    <div class="max-w-3xl mx-auto px-6 text-center">
      <h2 class="text-3xl font-bold mb-8">
        Installation
      </h2>

      <div class="bg-slate-900 border border-slate-800 rounded-2xl p-6 font-mono text-sm text-emerald-400">
        brew install pom-cli
      </div>

      <p class="mt-6 text-slate-400 text-sm">
        Also available via SDKMAN and direct binary download.
      </p>
    </div>
  </section>

  <!-- FOOTER -->
  <footer class="border-t border-slate-800 py-10 text-center text-slate-500 text-sm">
    © {new Date().getFullYear()} pom-cli. Open Source.
  </footer>

</div>
