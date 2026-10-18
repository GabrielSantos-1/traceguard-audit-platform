import { useState } from "react"
import { useAuth } from "./contexts/AuthContext"
import { RefreshProvider } from "./contexts/RefreshContext"
import LoginPage from "./pages/LoginPage"
import Header from "./components/Header"
import Sidebar, { type PageId } from "./components/Sidebar"
import KpiCards from "./components/KpiCards"
import SeverityChart from "./components/SeverityChart"
import TimelineChart from "./components/TimelineChart"
import RecentEvents from "./components/RecentEvents"
import CriticalAlerts from "./components/CriticalAlerts"
import Architecture from "./components/Architecture"
import Features from "./components/Features"
import TechStack from "./components/TechStack"
import Footer from "./components/Footer"
import EventosPage from "./pages/EventosPage"
import AlertasPage from "./pages/AlertasPage"
import RelatoriosPage from "./pages/RelatoriosPage"
import UsuariosPage from "./pages/UsuariosPage"
import TenantsPage from "./pages/TenantsPage"
import ConfiguracoesPage from "./pages/ConfiguracoesPage"
import IntegracoesPage from "./pages/IntegracoesPage"
import LogsPage from "./pages/LogsPage"

export default function App() {
  const { isAuthenticated, loading } = useAuth()
  const [page, setPage] = useState<PageId>("dashboard")

  if (loading) {
    return (
      <div className="min-h-screen bg-dark-950 flex items-center justify-center">
        <div className="animate-spin w-6 h-6 border-2 border-teal-500 border-t-transparent rounded-full" />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <LoginPage />
  }

  const renderPage = () => {
    switch (page) {
      case "dashboard":
        return (
          <>
            <KpiCards />
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
              <SeverityChart />
              <TimelineChart />
            </div>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
              <RecentEvents onNavigate={setPage} />
              <CriticalAlerts onNavigate={setPage} />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
              <Architecture />
              <Features />
              <TechStack />
            </div>
            <Footer />
          </>
        )
      case "eventos": return <EventosPage />
      case "alertas": return <AlertasPage />
      case "relatorios": return <RelatoriosPage />
      case "usuarios": return <UsuariosPage />
      case "tenants": return <TenantsPage />
      case "configuracoes": return <ConfiguracoesPage />
      case "integracoes": return <IntegracoesPage />
      case "logs": return <LogsPage />
    }
  }

  return (
    <RefreshProvider>
      <div className="flex h-screen bg-dark-950 text-gray-100 font-sans antialiased">
        <Sidebar activePage={page} onNavigate={setPage} />
        <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
          <Header onNavigate={setPage} />
          <main className="flex-1 overflow-y-auto p-5 space-y-5">
            {renderPage()}
          </main>
        </div>
      </div>
    </RefreshProvider>
  )
}
