import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import QualityConfirmDialog from '../QualityConfirmDialog';

describe('QualityConfirmDialog', () => {
  const defaultProps = {
    isOpen: true,
    currentQuality: '720p',
    targetQuality: '1080p',
    onConfirm: jest.fn(),
    onCancel: jest.fn(),
  };

  it('renders when open', () => {
    render(<QualityConfirmDialog {...defaultProps} />);
    expect(screen.getByText('确认画质切换')).toBeInTheDocument();
  });

  it('displays current and target quality values', () => {
    render(<QualityConfirmDialog {...defaultProps} />);
    expect(screen.getByText('720p')).toBeInTheDocument();
    expect(screen.getByText('1080p')).toBeInTheDocument();
  });

  it('displays current and target labels', () => {
    render(<QualityConfirmDialog {...defaultProps} />);
    expect(screen.getByText('当前画质')).toBeInTheDocument();
    expect(screen.getByText('目标画质')).toBeInTheDocument();
  });

  it('shows arrow between qualities', () => {
    render(<QualityConfirmDialog {...defaultProps} />);
    expect(screen.getByText('→')).toBeInTheDocument();
  });

  it('shows quality description for target quality', () => {
    render(<QualityConfirmDialog {...defaultProps} />);
    expect(screen.getByText(/高清画质/)).toBeInTheDocument();
  });

  it('shows 480p description when target is 480p', () => {
    render(<QualityConfirmDialog {...defaultProps} targetQuality="480p" />);
    expect(screen.getByText(/流畅画质/)).toBeInTheDocument();
  });

  it('shows 360p description when target is 360p', () => {
    render(<QualityConfirmDialog {...defaultProps} targetQuality="360p" />);
    expect(screen.getByText(/省流画质/)).toBeInTheDocument();
  });

  it('displays error message when provided', () => {
    render(<QualityConfirmDialog {...defaultProps} error="连接失败，请重试" />);
    expect(screen.getByText('连接失败，请重试')).toBeInTheDocument();
  });

  it('renders loading state with switch text', () => {
    render(<QualityConfirmDialog {...defaultProps} isLoading={true} />);
    expect(screen.getByText('切换中...')).toBeInTheDocument();
  });

  it('renders nothing when closed', () => {
    render(<QualityConfirmDialog {...defaultProps} isOpen={false} />);
    expect(screen.queryByText('确认画质切换')).not.toBeInTheDocument();
  });
});
