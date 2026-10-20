# Website deployment plan (draft)

**Status:** local preparation only. No remote repository was created or changed; Pages settings were not changed; no workflow was pushed, run, or deployed. **Reviewed:** 2026-10-20.

## Repository and site state checked

- The local project at `C:/Users/shubh/Shubh_ Active_projects/disha-voice` is an uncommitted Git repository on `master`, with no remote configured and no commit yet.
- Read-only GitHub checks under `shubhsaxena2020` found no owned `disha-voice` repository; `GET /repos/shubhsaxena2020/disha-voice` returned 404. Similar-name search results belong to other owners and are not treated as this project.
- The local `website/` currently has `index.html` and `styles.css`. The HTML links to `../README.md` and `../PROJECT_STATUS.md`; `README.md` is currently absent. These links escape the website root in a Pages artifact. The project status file also contains local development/environment details and must **not** be copied into a public site as a quick fix. The current privacy draft links back to that internal status file too.
- Accordingly, this is a proposal plus a **local workflow draft**, not evidence Pages is available or enabled. The workflow deliberately checks all local HTML/Markdown links and fails before artifact upload while these unsafe/unresolved references remain. Before enabling deployment, replace them with suitable public-site links or a reviewed, sanitized public status page, and verify the artifact contents.

## Recommended hosting

Use GitHub Pages with the GitHub Actions publishing source for this small, static, informational site, once the intended GitHub repository exists and its owner approves Pages. No external host, custom backend, accounts, analytics, comments, forms, image upload, or app telemetry are part of this plan. Do not deploy any private user media, credentials, local test reports, build outputs, or Android binaries as website content.

Pages serves static HTML/CSS/JavaScript; it is not an application server. Keep the website informational, with no sensitive transactions or collection of visitor data. GitHub's Pages policy disallows using it as a free host for a site primarily facilitating commercial transactions or SaaS. Visitors' IP addresses are logged by GitHub for security, so do not claim that the hosting service collects no data.

A Pages site is publicly accessible even when its source repository is private, except for eligible private publishing on GitHub Enterprise Cloud. For a personal GitHub Free account, Pages is available for public repositories; private Pages publishing requires an eligible organization/Enterprise Cloud setup. Confirm repository visibility and plan eligibility before enabling Pages. Do not make the source public until its entire history and proposed public content have had a secrets/privacy/license review.

A project Pages URL normally has the form `https://<owner>.github.io/<repository>/`; this depends on the verified owner/repository name. Do not publish or hard-code a URL until the remote exists and is checked. Ensure the site's links work under the project subpath, not just at a local filesystem URL.

## Proposed build and deployment flow

`.github/workflows/website.yml` is drafted to:

1. Validate on pull requests to `main` without deploying; run again on relevant pushes to `main`, or by manual dispatch.
2. Check out source with the read-only `GITHUB_TOKEN` and no persisted Git credentials.
3. Assemble a small `_site/` using an explicit allowlist: `website/index.html`, `website/styles.css`, and `docs/privacy/privacy-policy.md`. No project-status file, repository tree, build products, or hidden files are copied.
4. Check local HTML and Markdown links using Python's standard library. An unresolved or escaping reference fails the job before upload. The currently observed README/status references must be corrected first.
5. Upload the static artifact, then deploy it only when the event is not a PR and `github.ref` is exactly `refs/heads/main`. Deployment uses the `github-pages` environment and is serialized to avoid overlapping deployments.

The static build intentionally has no dependency installation or package-manager scripts. If the website later needs a generator/bundler or extra assets, add an explicit, reviewed build step and allowlisted output, then update the link checks and tests. Do not upload the entire repository as a shortcut.

### Workflow action pins

The draft pins every external action to the full commit SHA looked up from its official upstream release tag on 2026-10-20; the version comment makes maintenance reviewable:

| Action | Release | Full commit SHA |
|---|---|---|
| `actions/checkout` | `v7.0.1` | `3d3c42e5aac5ba805825da76410c181273ba90b1` |
| `actions/upload-pages-artifact` | `v5.0.0` | `fc324d3547104276b827a68afc52ff2a11cc49c9` |
| `actions/deploy-pages` | `v5.0.1` | `368f82528645a54fb793d4d04e342629a3f51346` |

These are immutable pins, not a promise to receive updates. Before enabling or maintaining the workflow, check upstream releases/security advisories, verify new SHAs belong to the official action repositories, review changes, and update the pin and version comment together. Pinning does not replace review of the workflow or action code.

### Permissions and deployment guardrails

- Workflow default: `contents: read`; build job has only `contents: read`. The deploy job explicitly grants only `pages: write` and `id-token: write`; these are required for Pages deployment and its OIDC identity. No PAT, repository secret, or write access to source contents is needed.
- Do not run privileged deployment on `pull_request` or use `pull_request_target`. The build job only copies and parses site files; it does not execute site HTML/JavaScript.
- In repository **Settings → Pages**, select **GitHub Actions** as the source. This setting is not changed by the workflow. Confirm Actions are permitted by repository/organization policy and that the `GITHUB_TOKEN` policy does not block the job's required Pages/OIDC permissions.
- In **Settings → Environments → `github-pages`**, restrict deployment branches to `main`; use any required reviewer/protection rule available to the repository. The job's own `main` check is an additional guard, not a substitute for environment protection.
- Protect `main` with reviewed changes before publishing. Review the exact `_site/` content, HTML links, policy language, licenses, and secrets before the first real deployment. After deployment is eventually authorized, verify the Pages deployment status and public URL by reading the repository's Pages/Actions state; a successful workflow file write is not proof of deployment.
- No repository settings, public repository, remote, Actions run, or website deployment was created or changed for this draft.

## GitHub Pages limits and suitability

GitHub's current Pages limits documentation lists: a **1 GB maximum published site**, a recommended **1 GB source-repository** limit, a **10-minute deployment timeout**, a *soft* **100 GB/month bandwidth** limit, and a *soft* **10 builds/hour** limit. The 10-build soft limit does not apply to sites built and published with a custom GitHub Actions workflow. Request rate limits may still apply and may return HTTP 429. Pages also allows at most one project site per repository and one user/organization site per account. These limits are generous for a small informational site, but avoid large media, APKs, model files, or generated reports; use another service if actual traffic or product needs exceed Pages' intended static-hosting use.

GitHub-hosted Actions usage for public repositories is free for standard runners; private-repository minutes/storage depend on the account plan and included quota. Check plan/billing limits if the future repository is private or Actions is otherwise billable.

## Before any first deployment

- Establish the intended repository with the owner, verify its exact owner/name/default branch and visibility, and ensure it actually exists. None of those remote prerequisites is established by this local draft.
- Review the full public commit history, website wording, assets, app privacy/safety claims, dependency licenses, and all files that will be public. Retain the existing clear statements that Disha Voice is an unverified prototype and not a mobility aid; do not overstate privacy, accessibility, accuracy, offline operation, or device support.
- Fix the README/status links in the website and the privacy draft without exposing local-only project status. Run the workflow's link gate and inspect its allowlisted artifact. Add an accessible, sanitized public status page only if it is useful and reviewed.
- Confirm Pages/Actions eligibility, select GitHub Actions as Pages source, apply environment branch restriction/protection, and get explicit authorization to push/merge the workflow to `main`. These are manual/remote steps and have not been performed.
- After an authorized first run, verify the exact deployment, its URL, base-path links, mobile layout, keyboard access, reduced-motion behavior, and public content. Until then, the site remains a local prototype; no deployment is claimed.

## Official references

- [GitHub Pages limits](https://docs.github.com/en/pages/getting-started-with-github-pages/github-pages-limits)
- [What is GitHub Pages?](https://docs.github.com/en/pages/getting-started-with-github-pages/what-is-github-pages)
- [Create a GitHub Pages site and eligibility](https://docs.github.com/en/pages/getting-started-with-github-pages/creating-a-github-pages-site)
- [Configure a publishing source, including custom Actions workflows](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site)
- [Using custom workflows with GitHub Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages)
- [Secure use reference: pin actions to full commit SHAs](https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions)
- [GITHUB_TOKEN permissions](https://docs.github.com/en/actions/security-for-github-actions/security-guides/automatic-token-authentication)
- [GitHub plans, Pages and Actions](https://docs.github.com/en/get-started/learning-about-github/githubs-plans)
- [Official checkout release](https://github.com/actions/checkout/releases/tag/v7.0.1)
- [Official upload-pages-artifact release](https://github.com/actions/upload-pages-artifact/releases/tag/v5.0.0)
- [Official deploy-pages release](https://github.com/actions/deploy-pages/releases/tag/v5.0.1)
