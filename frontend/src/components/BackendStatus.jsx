import { useEffect, useState } from 'react'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

function BackendStatus() {
    const [backendStatus, setBackendStatus] = useState('Checking...')

    useEffect(() => {
        fetch(`${apiBaseUrl}/boot/health`)
            .then((response) => {
                if (!response.ok) {
                    throw new Error('Request failed')
                }

                return response.json()
            })
            .then((data) => {
                setBackendStatus(data.status)
            })
            .catch(() => {
                setBackendStatus('UNAVAILABLE')
            })
    }, [])

    return <p>Backend status: {backendStatus}</p>
}

export default BackendStatus
