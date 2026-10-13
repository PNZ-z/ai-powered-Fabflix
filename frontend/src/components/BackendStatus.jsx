import { useEffect, useState } from 'react'
import { apiRequest } from '../api/apiClient.js'

function BackendStatus() {
    const [backendStatus, setBackendStatus] = useState('Checking...')

    useEffect(() => {
        apiRequest('/boot/health')
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
