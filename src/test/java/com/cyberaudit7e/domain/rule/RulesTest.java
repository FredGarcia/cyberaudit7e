package com.cyberaudit7e.domain.rule;

/* import com.cyberaudit7e.domain.enums.RuleCategory; */
/* import com.cyberaudit7e.dto.RuleResultDto; */
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests des règles d'audit M4.
 *
 * Pattern : on utilise Jsoup.parse(html) pour injecter un DOM
 * contrôlé dans l'AuditContext, sans faire de requête HTTP.
 * C'est l'équivalent d'un mock pour le crawler.
 */
@DisplayName("Règles d'audit M4 — DOM Jsoup")
class RulesTest {

    // ── Helper ──
    private AuditContext contextWithHtml(String html) {
        Document doc = Jsoup.parse(html);
        return AuditContext.withDocument("https://test.local", doc);
    }

    private AuditContext contextWithoutDocument() {
        return AuditContext.withoutDocument("https://test.local");
    }

    // ═══════════════════════════════════════════
    // RGAA-8.5 — Titre de page
    // ═══════════════════════════════════════════
    @Nested
    @DisplayName("TitlePresenceRule (RGAA-8.5)")
    class TitleTests {
        private final TitlePresenceRule rule = new TitlePresenceRule();

        @Test void shouldPassWithGoodTitle() {
            var result = rule.evaluate(contextWithHtml(
                    "<html><head><title>Bienvenue sur mon site</title></head></html>"));
            assertTrue(result.passed());
            assertEquals(1.0, result.score());
            assertTrue(result.detail().contains("Bienvenue"));
        }

        @Test void shouldFailWithEmptyTitle() {
            var result = rule.evaluate(contextWithHtml(
                    "<html><head><title></title></head></html>"));
            assertFalse(result.passed());
            assertEquals(0.0, result.score());
        }

        @Test
        void shouldPartialWithShortTitle() {
            var result = rule.evaluate(contextWithHtml(
                    "<html><head><title>Hi</title></head></html>"));
            assertEquals(0.5, result.score());
            assertTrue(result.detail().contains("trop court"));
        }

        @Test void shouldPartialWithGenericTitle() {
            var result = rule.evaluate(contextWithHtml(
                    "<html><head><title>Accueil</title></head></html>"));
            assertEquals(0.4, result.score());
        }

        @Test void shouldFailWithoutDocument() {
            var result = rule.evaluate(contextWithoutDocument());
            assertFalse(result.passed());
            assertTrue(result.detail().contains("crawler"));
        }
    }

    // ═══════════════════════════════════════════
    // RGAA-8.3 — Attribut lang
    // ═══════════════════════════════════════════
    @Nested
    @DisplayName("LangAttributeRule (RGAA-8.3)")
    class LangTests {
        private final LangAttributeRule rule = new LangAttributeRule();

        @Test void shouldPassWithLangFr() {
            var result = rule.evaluate(contextWithHtml(
                    "<html lang='fr'><head></head><body></body></html>"));
            assertTrue(result.passed());
            assertTrue(result.detail().contains("fr"));
        }

        @Test void shouldPassWithLangEnUS() {
            var result = rule.evaluate(contextWithHtml(
                    "<html lang='en-US'><head></head><body></body></html>"));
            assertTrue(result.passed());
        }

        @Test void shouldFailWithoutLang() {
            var result = rule.evaluate(contextWithHtml(
                    "<html><head></head><body></body></html>"));
            assertFalse(result.passed());
            assertEquals(0.0, result.score());
        }

        @Test void shouldPartialWithBadFormat() {
            var result = rule.evaluate(contextWithHtml(
                    "<html lang='français'><head></head><body></body></html>"));
            assertEquals(0.5, result.score());
        }
    }

    // ═══════════════════════════════════════════
    // RGAA-1.1 — Alt-text des images
    // ═══════════════════════════════════════════
    @Nested
    @DisplayName("ImageAltRule (RGAA-1.1)")
    class ImageTests {
        private final ImageAltRule rule = new ImageAltRule();

        @Test void shouldPassWithAllAlts() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <img src="a.jpg" alt="Photo A">
                    <img src="b.jpg" alt="Photo B">
                </body></html>"""));
            assertTrue(result.passed());
            assertEquals(1.0, result.score());
        }

        @Test void shouldPassWithNoImages() {
            var result = rule.evaluate(contextWithHtml(
                    "<html><body><p>Pas d'images</p></body></html>"));
            assertTrue(result.passed());
            assertTrue(result.detail().contains("N/A"));
        }

        @Test
        void shouldPartialWithMissingAlt() {
            var result = rule.evaluate(contextWithHtml("""
                    <html><body>
                        <img src="a.jpg" alt="OK">
                        <img src="b.jpg">
                    </body></html>"""));
            assertEquals(0.5, result.score());
            assertTrue(result.detail().contains("1 sans alt"));
        }

        @Test void shouldCountEmptyAltAsDecorative() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <img src="deco.jpg" alt="">
                </body></html>"""));
            assertTrue(result.passed());
            assertTrue(result.detail().contains("alt vide"));
        }
    }

    // ═══════════════════════════════════════════
    // RGAA-9.1 — Hiérarchie des titres
    // ═══════════════════════════════════════════
    @Nested
    @DisplayName("HeadingStructureRule (RGAA-9.1)")
    class HeadingTests {
        private final HeadingStructureRule rule = new HeadingStructureRule();

        @Test void shouldPassWithCorrectHierarchy() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <h1>Titre principal</h1>
                    <h2>Section A</h2>
                    <h3>Sous-section</h3>
                    <h2>Section B</h2>
                </body></html>"""));
            assertTrue(result.passed());
            assertTrue(result.detail().contains("h1 unique OK"));
            assertTrue(result.detail().contains("hiérarchie continue"));
        }

        @Test void shouldDetectMultipleH1() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <h1>Premier H1</h1>
                    <h1>Deuxième H1</h1>
                </body></html>"""));
            assertTrue(result.score() < 1.0);
            assertTrue(result.detail().contains("h1: 2"));
        }

        @Test void shouldDetectLevelSkips() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <h1>Titre</h1>
                    <h3>Saut de h2 !</h3>
                </body></html>"""));
            assertTrue(result.score() < 1.0);
            assertTrue(result.detail().contains("saut"));
        }

        @Test void shouldPartialWithNoHeadings() {
            var result = rule.evaluate(contextWithHtml(
                    "<html><body><p>Pas de titres</p></body></html>"));
            assertEquals(0.3, result.score());
        }
    }

    // ═══════════════════════════════════════════
    // RGAA-11.1 — Étiquettes de formulaires
    // ═══════════════════════════════════════════
    @Nested
    @DisplayName("FormLabelRule (RGAA-11.1)")
    class FormTests {
        private final FormLabelRule rule = new FormLabelRule();

        @Test void shouldPassWithLabels() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <label for="name">Nom</label>
                    <input id="name" type="text">
                </body></html>"""));
            assertTrue(result.passed());
        }

        @Test void shouldPassWithAriaLabel() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <input type="text" aria-label="Recherche">
                </body></html>"""));
            assertTrue(result.passed());
        }

        @Test void shouldPassWithImplicitLabel() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <label>Nom <input type="text"></label>
                </body></html>"""));
            assertTrue(result.passed());
        }

        @Test void shouldFailWithoutLabel() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <input type="text" placeholder="Sans label">
                </body></html>"""));
            assertFalse(result.passed());
        }

        @Test void shouldIgnoreHiddenAndButtons() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <input type="hidden" value="token">
                    <input type="submit" value="OK">
                    <input type="button" value="Annuler">
                </body></html>"""));
            assertTrue(result.passed());
            assertTrue(result.detail().contains("N/A"));
        }
    }

    // ═══════════════════════════════════════════
    // WCAG-1.3.1 — Landmarks ARIA
    // ═══════════════════════════════════════════
    @Nested
    @DisplayName("AriaLandmarkRule (WCAG-1.3.1)")
    class LandmarkTests {
        private final AriaLandmarkRule rule = new AriaLandmarkRule();

        @Test void shouldPassWithAllLandmarks() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <header>En-tête</header>
                    <nav>Navigation</nav>
                    <main>Contenu</main>
                    <footer>Pied</footer>
                </body></html>"""));
            assertTrue(result.passed());
            assertEquals(1.0, result.score());
        }

        @Test void shouldPartialWithMissingLandmarks() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <main>Contenu seul</main>
                </body></html>"""));
            assertEquals(0.25, result.score());
        }

        @Test void shouldDetectAriaRoles() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <div role="banner">Header</div>
                    <div role="navigation">Nav</div>
                    <div role="main">Main</div>
                    <div role="contentinfo">Footer</div>
                </body></html>"""));
            assertTrue(result.passed());
        }
    }

    // ═══════════════════════════════════════════
    // WCAG-1.4.4 — Meta viewport
    // ═══════════════════════════════════════════
    @Nested
    @DisplayName("MetaViewportRule (WCAG-1.4.4)")
    class ViewportTests {
        private final MetaViewportRule rule = new MetaViewportRule();

        @Test void shouldPassWithGoodViewport() {
            var result = rule.evaluate(contextWithHtml("""
                <html><head>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                </head><body></body></html>"""));
            assertTrue(result.passed());
        }

        @Test void shouldFailWithZoomBlocked() {
            var result = rule.evaluate(contextWithHtml("""
                <html><head>
                    <meta name="viewport" content="width=device-width, user-scalable=no">
                </head><body></body></html>"""));
            assertFalse(result.passed());
            assertTrue(result.detail().contains("user-scalable=no"));
        }

        @Test void shouldFailWithMaxScale1() {
            var result = rule.evaluate(contextWithHtml("""
                <html><head>
                    <meta name="viewport" content="width=device-width, maximum-scale=1.0">
                </head><body></body></html>"""));
            assertFalse(result.passed());
            assertTrue(result.detail().contains("maximum-scale"));
        }

        @Test void shouldPartialWithNoViewport() {
            var result = rule.evaluate(contextWithHtml(
                    "<html><head></head><body></body></html>"));
            assertEquals(0.6, result.score());
        }
    }

    // ═══════════════════════════════════════════
    // WCAG-2.4.4 — Intitulé des liens
    // ═══════════════════════════════════════════
    @Nested
    @DisplayName("LinkPurposeRule (WCAG-2.4.4)")
    class LinkTests {
        private final LinkPurposeRule rule = new LinkPurposeRule();

        @Test void shouldPassWithDescriptiveLinks() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <a href="/about">À propos de notre association</a>
                    <a href="/contact">Nous contacter</a>
                </body></html>"""));
            assertTrue(result.passed());
        }

        @Test void shouldDetectVagueLinks() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <a href="/page">Cliquez ici</a>
                    <a href="/more">En savoir plus</a>
                    <a href="/about">Notre mission</a>
                </body></html>"""));
            assertTrue(result.score() < 1.0);
            assertTrue(result.detail().contains("2 vague"));
        }

        @Test void shouldDetectEmptyLinks() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <a href="/nowhere"></a>
                </body></html>"""));
            assertTrue(result.detail().contains("1 vide"));
        }

        @Test void shouldAcceptImageLinksWithAlt() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <a href="/home"><img alt="Accueil" src="logo.png"></a>
                </body></html>"""));
            assertTrue(result.passed());
        }
    }

    // ═══════════════════════════════════════════
    // DSFR — En-tête et pied de page
    // ═══════════════════════════════════════════
    @Nested
    @DisplayName("DsfrHeaderRule (DSFR-HDR-01)")
    class DsfrHeaderTests {
        private final DsfrHeaderRule rule = new DsfrHeaderRule();

        @Test void shouldDetectFullDsfrHeader() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <header class="fr-header">
                        <div class="fr-logo">République Française</div>
                        <div class="fr-header__service">Mon Service</div>
                        <nav class="fr-nav">Navigation</nav>
                    </header>
                    <link href="/css/dsfr.min.css" rel="stylesheet">
                </body></html>"""));
            assertTrue(result.passed());
        }

        @Test void shouldFailWithoutDsfr() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <header><h1>Site normal</h1></header>
                </body></html>"""));
            assertFalse(result.passed());
        }

        @Test void shouldFailWithoutDocument() {
            var result = rule.evaluate(contextWithoutDocument());
            assertFalse(result.passed());
            assertTrue(result.detail().contains("crawler"));
        }
    }

    @Nested
    @DisplayName("DsfrFooterRule (DSFR-FTR-01)")
    class DsfrFooterTests {
        private final DsfrFooterRule rule = new DsfrFooterRule();

        @Test void shouldDetectFullDsfrFooter() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <footer class="fr-footer">
                        <a href="/mentions">Mentions légales</a>
                        <a href="/accessibilite">Accessibilité : partiellement conforme</a>
                        <a href="/plan">Plan du site</a>
                        <a href="/donnees">Données personnelles</a>
                    </footer>
                </body></html>"""));
            assertTrue(result.passed());
        }

        @Test void shouldPartialWithIncompleteFooter() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <footer class="fr-footer">
                        <a href="/mentions">Mentions légales</a>
                    </footer>
                </body></html>"""));
            assertTrue(result.score() > 0.0);
            assertTrue(result.score() < 1.0);
        }
    }

    // ═══════════════════════════════════════════
    // DSFR — Fil d'Ariane
    // ═══════════════════════════════════════════
    @Nested
    @DisplayName("DsfrBreadcrumbRule (DSFR-BRD-01)")
    class BreadcrumbTests {
        private final DsfrBreadcrumbRule rule = new DsfrBreadcrumbRule();

        @Test void shouldPassWithFullDsfrBreadcrumb() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <nav class="fr-breadcrumb" aria-label="Fil d'Ariane">
                        <ol>
                            <li><a href="/">Accueil</a></li>
                            <li>Page courante</li>
                        </ol>
                    </nav>
                </body></html>"""));
            assertTrue(result.passed());
        }

        @Test void shouldPartialWithGenericBreadcrumb() {
            var result = rule.evaluate(contextWithHtml("""
                <html><body>
                    <div class="breadcrumb">Accueil > Page</div>
                </body></html>"""));
            assertEquals(0.5, result.score());
            assertTrue(result.detail().contains("générique"));
        }

        @Test void shouldFailWithNoBreadcrumb() {
            var result = rule.evaluate(contextWithHtml(
                    "<html><body><p>Pas de fil</p></body></html>"));
            assertFalse(result.passed());
            assertEquals(0.0, result.score());
        }
    }

    // ═══════════════════════════════════════════
    // AuditContext
    // ═══════════════════════════════════════════
    @Nested
    @DisplayName("AuditContext")
    class ContextTests {

        @Test void shouldCreateWithDocument() {
            Document doc = Jsoup.parse("<html></html>");
            AuditContext ctx = AuditContext.withDocument("https://test.fr", doc);
            assertTrue(ctx.hasDocument());
            assertEquals("https://test.fr", ctx.url());
        }

        @Test void shouldCreateWithoutDocument() {
            AuditContext ctx = AuditContext.withoutDocument("https://test.fr");
            assertFalse(ctx.hasDocument());
            assertEquals("https://test.fr", ctx.url());
        }
    }
}
