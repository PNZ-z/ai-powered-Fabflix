import { useEffect, useRef } from 'react'

const siteKey = import.meta.env.VITE_RECAPTCHA_SITE_KEY
const scriptId = 'fabflix-recaptcha-script'

function RecaptchaField({ onTokenChange, resetSignal }) {
  const containerRef = useRef(null)
  const widgetIdRef = useRef(null)

  useEffect(() => {
    if (!siteKey) return undefined

    function renderWidget() {
      if (!containerRef.current || widgetIdRef.current !== null || !window.grecaptcha) return

      widgetIdRef.current = window.grecaptcha.render(containerRef.current, {
        sitekey: siteKey,
        callback: onTokenChange,
        'expired-callback': () => onTokenChange(''),
        'error-callback': () => onTokenChange(''),
      })
    }

    if (window.grecaptcha) {
      window.grecaptcha.ready(renderWidget)
      return undefined
    }

    let script = document.getElementById(scriptId)
    if (!script) {
      script = document.createElement('script')
      script.id = scriptId
      script.src = 'https://www.google.com/recaptcha/api.js?render=explicit'
      script.async = true
      script.defer = true
      document.head.appendChild(script)
    }
    script.addEventListener('load', renderWidget)

    return () => script.removeEventListener('load', renderWidget)
  }, [onTokenChange])

  useEffect(() => {
    if (widgetIdRef.current !== null && window.grecaptcha) {
      window.grecaptcha.reset(widgetIdRef.current)
    }
  }, [resetSignal])

  if (!siteKey) return null

  return <div className="recaptcha-field" ref={containerRef} />
}

export default RecaptchaField
